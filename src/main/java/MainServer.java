import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MainServer {
    private static final List<Disaster> activeDisasters = new CopyOnWriteArrayList<>();
    private static final String FILE_NAME = "alerts_history.csv";

    public static void main(String[] args) throws IOException {
        loadFromFile();

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new UIHandler());
        server.createContext("/dashboard", new DashboardHandler());
        server.createContext("/addAlert", new AddAlertHandler());
        server.setExecutor(null); 
        server.start();
        System.out.println("Global Disaster Server started on port " + port);

        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:8080"));
            }
        } catch (Exception e) {
            System.out.println("Could not auto-open browser. Please manually go to http://localhost:8080");
        }

        Thread usgsFeedThread = new Thread(() -> {
            fetchUSGSApi();
            while (true) {
                try {
                    Thread.sleep(60000); 
                    fetchUSGSApi();
                } catch (Exception ignored) {}
            }
        });
        usgsFeedThread.setDaemon(true);
        usgsFeedThread.start();
    }

    private static String fetchWeather(double lat, double lon) {
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            in.close();
            
            String json = response.toString();
            int currentBlock = json.indexOf("\"current_weather\":{");
            if (currentBlock != -1) {
                int idx = json.indexOf("\"temperature\":", currentBlock);
                if (idx != -1) {
                    int end = json.indexOf(",", idx);
                    String temp = json.substring(idx + 14, end);
                    int windIdx = json.indexOf("\"windspeed\":", currentBlock);
                    String wind = "";
                    if (windIdx != -1) {
                        int wEnd = json.indexOf(",", windIdx);
                        wind = " | Wind: " + json.substring(windIdx + 12, wEnd) + "km/h";
                    }
                    return temp + "\u00B0C" + wind;
                }
            }
        } catch (Exception e) {}
        return "Unknown";
    }

    private static synchronized void fetchUSGSApi() {
        try {
            System.out.println(">> Fetching REAL Pakistan data from USGS...");
            URL url = new URL("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&minlatitude=23.5&maxlatitude=37.1&minlongitude=60.8&maxlongitude=77.8&limit=100");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            parseUSGSJson(response.toString());
        } catch (Exception e) {
            System.out.println("API Fetch Error: " + e.getMessage());
        }
    }

    private static void parseUSGSJson(String json) {
        String[] features = json.split("\"type\":\"Feature\"");
        int count = 0;
        for (int i = 1; i < features.length; i++) {
            if (count >= 15) break; // LIMIT TO 15 FASTEST ALERTS!
            count++;
            try {
                String f = features[i];
                String place = extractString(f, "\"place\":\"");
                
                String lowerPlace = place.toLowerCase();
                if (lowerPlace.contains("afghanistan") || lowerPlace.contains("india") || lowerPlace.contains("iran") || lowerPlace.contains("china")) {
                    continue; 
                }

                double mag = extractDouble(f, "\"mag\":");
                long timeMs = (long) extractDouble(f, "\"time\":");
                LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMs), ZoneId.systemDefault());
                String timestamp = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                int coordsIdx = f.indexOf("\"coordinates\":[");
                String coordsStr = f.substring(coordsIdx + 15, f.indexOf("]", coordsIdx));
                String[] coords = coordsStr.split(",");
                double lon = Double.parseDouble(coords[0].trim());
                double lat = Double.parseDouble(coords[1].trim());

                int sev = mag >= 6.0 ? 5 : (mag >= 5.0 ? 4 : 3);
                
                boolean exists = activeDisasters.stream().anyMatch(d -> d.getLocation().equals(place) && d.getTimestamp().equals(timestamp));
                if (!exists) {
                    Earthquake eq = new Earthquake(place, sev, timestamp, lat, lon, mag);
                    eq.setWeatherTemp(fetchWeather(lat, lon));
                    addAlertAndSave(eq);
                }
            } catch (Exception ignored) { }
        }
    }

    private static double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0.0;
        int start = idx + key.length();
        int end = json.indexOf(",", start);
        if (json.charAt(start) == 'n') return 0.0; 
        return Double.parseDouble(json.substring(start, end));
    }

    private static String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return "Unknown";
        int start = idx + key.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private static synchronized void addAlertAndSave(Disaster d) {
        activeDisasters.add(d);
        try (FileWriter fw = new FileWriter(FILE_NAME, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(d.toCSV());
        } catch (IOException ignored) {}
    }

    private static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    String type = parts[0];
                    String loc = parts[1];
                    int sev = Integer.parseInt(parts[2]);
                    String time = parts[3];
                    double lat = Double.parseDouble(parts[4]);
                    double lon = Double.parseDouble(parts[5]);
                    String extra = parts.length > 6 ? parts[6] : "0";
                    if (type.equals("Earthquake")) activeDisasters.add(new Earthquake(loc, sev, time, lat, lon, Double.parseDouble(extra)));
                    else if (type.equals("Flood")) activeDisasters.add(new Flood(loc, sev, time, lat, lon, Double.parseDouble(extra)));
                    else if (type.equals("Wildfire")) activeDisasters.add(new Wildfire(loc, sev, time, lat, lon, extra));
                    else if (type.equals("Tsunami")) activeDisasters.add(new Tsunami(loc, sev, time, lat, lon, Double.parseDouble(extra)));
                    else if (type.equals("Heatwave")) activeDisasters.add(new Heatwave(loc, sev, time, lat, lon, Double.parseDouble(extra)));
                    else if (type.equals("Landslide")) activeDisasters.add(new Landslide(loc, sev, time, lat, lon, extra));
                    else activeDisasters.add(new Disaster(loc, sev, time, lat, lon));
                }
            }
        } catch (Exception e) {}
    }

    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String html = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Global Disaster</title>" +
                          "<link href='https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap' rel='stylesheet'>" +
                          "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'>" +
                          "<style>" +
                          "body { font-family: 'Roboto', sans-serif; background: #0b0f19; color: #fff; margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center; }" +
                          ".container { background: #1a2235; padding: 40px; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); text-align: center; max-width: 450px; width: 90%; border: 1px solid #2a3449; }" +
                          ".icon-header { font-size: 60px; color: #00e5ff; margin-bottom: 20px; text-shadow: 0 0 15px rgba(0,229,255,0.5); }" +
                          "h1 { font-size: 2.2rem; margin: 0 0 10px 0; font-weight: 700; color:#fff; letter-spacing: 1px; }" +
                          "p { color: #8a94a6; margin-bottom: 30px; font-size: 1rem; }" +
                          "input[type=text] { width: 100%; padding: 15px; border-radius: 6px; border: 1px solid #2a3449; background: #0b0f19; color: #fff; font-size: 1rem; box-sizing: border-box; outline: none; margin-bottom:20px; transition: 0.3s; }" +
                          "input[type=text]:focus { border-color: #00e5ff; box-shadow: 0 0 8px rgba(0,229,255,0.3); }" +
                          "button { width: 100%; padding: 15px; background: #00e5ff; color: #000; border: none; border-radius: 6px; font-size: 1.1rem; font-weight: 700; cursor: pointer; transition: 0.3s; text-transform: uppercase; letter-spacing: 1px; }" +
                          "button:hover { background: #00b8cc; box-shadow: 0 0 15px rgba(0,229,255,0.4); }" +
                          "</style></head>" +
                          "<body>" +
                          "<div class='container'>" +
                          "<div class='icon-header'><i class='fa-solid fa-earth-americas'></i></div>" +
                          "<h1>GLOBAL DISASTER</h1>" +
                          "<p>Advanced Satellite Monitoring System</p>" +
                          "<form action='/dashboard' method='get'>" +
                          "<input type='text' name='username' placeholder='Operator Name' required />" +
                          "<button type='submit'>Initialize System</button>" +
                          "</form>" +
                          "</div>" +
                          "</body></html>";

            byte[] responseBytes = html.getBytes("UTF-8");
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = t.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                String query = t.getRequestURI().getQuery();
                String userName = "Operator";
                String filter = "All";
                String locFilter = "All";
                
                if (query != null) {
                    String[] parts = query.split("&");
                    for (String p : parts) {
                        if (p.startsWith("username=")) {
                            userName = p.split("=").length > 1 ? p.split("=")[1] : "Operator";
                            try { userName = URLDecoder.decode(userName.replace("+", " "), "UTF-8"); } catch(Exception e) {}
                        }
                        if (p.startsWith("filter=")) filter = p.split("=").length > 1 ? p.split("=")[1] : "All";
                        if (p.startsWith("locFilter=")) locFilter = p.split("=").length > 1 ? p.split("=")[1] : "All";
                    }
                }

                List<Disaster> filteredList = activeDisasters;
                
                if (!filter.equals("All")) {
                    final String currentFilter = filter;
                    filteredList = filteredList.stream().filter(d -> d.getType().equals(currentFilter)).collect(Collectors.toList());
                }

                if (!locFilter.equals("All")) {
                    final String currentLoc = locFilter.toLowerCase();
                    filteredList = filteredList.stream().filter(d -> d.getLocation() != null && d.getLocation().toLowerCase().contains(currentLoc)).collect(Collectors.toList());
                }

                int eqCount = 0, flCount = 0, wfCount = 0, hwCount = 0, lsCount = 0;
                for(Disaster d : filteredList) {
                    switch(d.getType()) {
                        case "Earthquake": eqCount++; break;
                        case "Flood": flCount++; break;
                        case "Wildfire": wfCount++; break;
                        case "Heatwave": hwCount++; break;
                        case "Landslide": lsCount++; break;
                    }
                }

                List<Disaster> reverseList = new ArrayList<>(filteredList);
                Collections.reverse(reverseList);
                if (reverseList.size() > 50) reverseList = reverseList.subList(0, 50);

                StringBuilder alertsHtml = new StringBuilder();
                StringBuilder mapMarkersJS = new StringBuilder();

                for (Disaster d : reverseList) {
                    alertsHtml.append(d.generateAlert());
                    String color = d.getSeverityLevel() >= 4 ? "#ff3333" : "#ffaa00";
                    String safeLoc = d.getLocation() != null ? d.getLocation().replace("'", "\\'") : "Unknown";
                    mapMarkersJS.append("L.circleMarker([").append(d.getLat()).append(", ").append(d.getLon())
                                .append("], {color: '").append(color).append("', radius: 6, fillOpacity: 0.8, weight: 2})")
                                .append(".bindPopup('<b style=\"color:#000;\">").append(d.getType()).append("</b><br><span style=\"color:#333;\">").append(safeLoc).append("</span>')")
                                .append(".addTo(map);\n");
                }

                if (alertsHtml.length() == 0) alertsHtml.append("<p style='color:#8a94a6;'>No data available in selected sector.</p>");

                String html = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Global Disaster Monitor</title>" +
                              "<link href='https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap' rel='stylesheet'>" +
                              "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'>" +
                              "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />" +
                              "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                              "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                              "<style>" +
                              "body { font-family: 'Roboto', sans-serif; background: #0b0f19; color: #e2e8f0; margin: 0; min-height: 100vh; }" +
                              ".topbar { background: #111827; border-bottom: 1px solid #1f2937; padding: 15px 30px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }" +
                              ".topbar h1 { margin: 0; font-size: 1.5rem; font-weight: 500; color: #00e5ff; display:flex; align-items:center; gap:10px; }" +
                              ".topbar a { color: #fff; text-decoration: none; font-weight: 500; background: #ef4444; padding: 8px 16px; border-radius: 6px; transition: 0.2s; font-size: 0.9rem; }" +
                              ".topbar a:hover { background: #dc2626; box-shadow: 0 0 10px rgba(239, 68, 68, 0.4); }" +
                              ".container { max-width: 1300px; margin: 20px auto; padding: 0 20px; display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }" +
                              ".card { background: #111827; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.2); padding: 20px; margin-bottom: 20px; border: 1px solid #1f2937; }" +
                              "#map { height: 450px; border-radius: 8px; background: #1a2235; z-index:1; border: 1px solid #374151; margin-bottom: 15px; }" +
                              "h2 { margin-top:0; font-size: 1.2rem; border-bottom: 1px solid #1f2937; padding-bottom: 10px; color: #fff; display:flex; align-items:center; gap:8px; }" +
                              ".alert-card { border: 1px solid #1f2937; padding: 15px; border-radius: 8px; margin-bottom: 15px; background: #1a2235; border-left: 4px solid #00e5ff; transition: 0.2s; }" +
                              ".alert-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.2); }" +
                              ".alert-card.severity-high { border-left-color: #ef4444; background: rgba(239, 68, 68, 0.05); }" +
                              ".alert-title { margin: 0 0 5px 0; font-size: 1.1rem; font-weight:500; color:#fff; }" +
                              ".alert-meta { font-size: 0.85rem; color: #94a3b8; margin: 0 0 10px 0; }" +
                              ".action-box { font-size: 0.85rem; padding: 10px; border-radius: 6px; font-weight:500; background: rgba(0, 229, 255, 0.1); color: #00e5ff; }" +
                              ".action-box.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }" +
                              ".filters a { display:inline-block; padding: 6px 14px; margin: 0 5px 10px 0; background: #1f2937; color: #94a3b8; text-decoration: none; border-radius: 20px; font-size: 0.85rem; transition: 0.2s; }" +
                              ".filters a:hover { background: #374151; color: #fff; }" +
                              ".filters a.active { background: #00e5ff; color: #000; font-weight: 500; }" +
                              ".province-filters { background: #0b0f19; padding: 10px; border-radius: 8px; margin-bottom: 15px; border: 1px solid #1f2937; }" +
                              ".province-filters span { color: #00e5ff; font-weight: 500; margin-right: 10px; font-size: 0.9rem; }" +
                              "input, select { width: 100%; padding: 12px; margin-bottom: 15px; border: 1px solid #374151; border-radius: 6px; box-sizing: border-box; font-family:'Roboto'; background: #0b0f19; color: #fff; outline: none; }" +
                              "input:focus, select:focus { border-color: #00e5ff; }" +
                              "button.btn-submit { width: 100%; padding: 12px; background: #00e5ff; color: #000; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; transition: 0.2s; text-transform: uppercase; letter-spacing: 1px; }" +
                              "button.btn-submit:hover { background: #00b8cc; box-shadow: 0 0 12px rgba(0,229,255,0.4); }" +
                              "</style>" +
                              "</head>" +
                              "<body>" +
                              "<div class='topbar'>" +
                              "<h1><i class='fa-solid fa-satellite-dish'></i> GLOBAL DISASTER</h1>" +
                              "<div><span style='margin-right:20px; color:#94a3b8;'><i class='fa-solid fa-user-shield'></i> " + userName + "</span><a href='/'><i class='fa-solid fa-right-from-bracket'></i> Disconnect</a></div>" +
                              "</div>" +
                              "<div class='container'>" +
                              "<div class='left-column'>" +
                              "<div class='card' style='padding:5px;'>" +
                              "<div id='map'></div>" +
                              "</div>" +
                              "<div class='card'>" +
                              "<h2><i class='fa-solid fa-tower-cell'></i> Live Satellite Feed</h2>" +
                              "<div class='province-filters'>" +
                              "<span><i class='fa-solid fa-location-dot'></i> Province Select:</span>" +
                              "<a href='/dashboard?username=" + userName + "&filter=" + filter + "&locFilter=All' class='filters'><span style='" + (locFilter.equals("All")?"color:#fff;":"") + "'>All Pakistan</span></a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=" + filter + "&locFilter=Sindh' class='filters'><span style='" + (locFilter.equals("Sindh")?"color:#fff;":"") + "'>Sindh</span></a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=" + filter + "&locFilter=Punjab' class='filters'><span style='" + (locFilter.equals("Punjab")?"color:#fff;":"") + "'>Punjab</span></a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=" + filter + "&locFilter=Balochistan' class='filters'><span style='" + (locFilter.equals("Balochistan")?"color:#fff;":"") + "'>Balochistan</span></a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=" + filter + "&locFilter=Khyber' class='filters'><span style='" + (locFilter.equals("Khyber")?"color:#fff;":"") + "'>KPK</span></a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=" + filter + "&locFilter=Gilgit' class='filters'><span style='" + (locFilter.equals("Gilgit")?"color:#fff;":"") + "'>Gilgit</span></a>" +
                              "</div>" +
                              "<div class='filters'>" +
                              "<a href='/dashboard?username=" + userName + "&filter=All&locFilter=" + locFilter + "' class='" + (filter.equals("All")?"active":"") + "'>All Alerts</a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=Earthquake&locFilter=" + locFilter + "' class='" + (filter.equals("Earthquake")?"active":"") + "'>Earthquakes</a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=Flood&locFilter=" + locFilter + "' class='" + (filter.equals("Flood")?"active":"") + "'>Floods</a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=Wildfire&locFilter=" + locFilter + "' class='" + (filter.equals("Wildfire")?"active":"") + "'>Wildfires</a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=Heatwave&locFilter=" + locFilter + "' class='" + (filter.equals("Heatwave")?"active":"") + "'>Heatwaves</a>" +
                              "<a href='/dashboard?username=" + userName + "&filter=Landslide&locFilter=" + locFilter + "' class='" + (filter.equals("Landslide")?"active":"") + "'>Landslides</a>" +
                              "</div>" +
                              alertsHtml.toString() +
                              "</div>" +
                              "</div>" +
                              "<div class='right-column'>" +
                              
                              "<div class='card' style='text-align:center; padding-bottom:30px;'>" +
                              "<h2><i class='fa-solid fa-chart-pie'></i> Live Analytics</h2>" +
                              "<canvas id='disasterChart'></canvas>" +
                              "</div>" +

                              "<div class='card add-form'>" +
                              "<h2><i class='fa-solid fa-plus'></i> Manual Override</h2>" +
                              "<form action='/addAlert' method='post'>" +
                              "<input type='hidden' name='username' value='" + userName + "' />" +
                              "<label style='font-size:0.85rem;color:#94a3b8;margin-bottom:5px;display:block;'>Disaster Type</label>" +
                              "<select name='type'>" +
                              "<option value='Earthquake'>Earthquake</option>" +
                              "<option value='Flood'>Flood Warning</option>" +
                              "<option value='Wildfire'>Wildfire Event</option>" +
                              "<option value='Heatwave'>Heatwave Alert</option>" +
                              "<option value='Landslide'>Landslide Risk</option>" +
                              "</select>" +
                              "<label style='font-size:0.85rem;color:#94a3b8;margin-bottom:5px;display:block;'>Location / Province</label><select name='location'><option value='Pakistan (General)'>Pakistan (General)</option><option value='Sindh'>Sindh</option><option value='Punjab'>Punjab</option><option value='Balochistan'>Balochistan</option><option value='Khyber Pakhtunkhwa (KPK)'>Khyber Pakhtunkhwa (KPK)</option><option value='Gilgit-Baltistan'>Gilgit-Baltistan</option><option value='Azad Kashmir'>Azad Kashmir</option><option value='Islamabad (ICT)'>Islamabad (ICT)</option></select>" +
                              "<label style='font-size:0.85rem;color:#94a3b8;margin-bottom:5px;display:block;'>Severity (1-5)</label><input type='number' name='severity' min='1' max='5' required />" +
                              "<label style='font-size:0.85rem;color:#94a3b8;margin-bottom:5px;display:block;'>Extra Details</label><input type='text' name='extra' required />" +
                              "<button type='submit' class='btn-submit'>Deploy Alert</button>" +
                              "</form>" +
                              "</div>" +
                              "</div>" +
                              "</div>" +
                              "<script>" +
                              "var map = L.map('map').setView([30.0, 69.0], 5);" + 
                              "L.tileLayer('http://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', { maxZoom: 20, subdomains:['mt0','mt1','mt2','mt3'], attribution: 'Global Tracking' }).addTo(map);" +
                              mapMarkersJS.toString() +
                              "var ctx = document.getElementById('disasterChart').getContext('2d');" +
                              "var myChart = new Chart(ctx, {" +
                              "type: 'doughnut'," +
                              "data: { labels: ['Earthquakes', 'Floods', 'Wildfires', 'Heatwaves', 'Landslides'], datasets: [{ data: ["+eqCount+","+flCount+","+wfCount+","+hwCount+","+lsCount+"], backgroundColor: ['#ef4444', '#3b82f6', '#f97316', '#eab308', '#8b4513'], borderColor:'#111827', borderWidth:2 }] }," +
                              "options: { responsive: true, plugins: { legend: { position: 'bottom', labels: {color: '#e2e8f0', padding: 20, font:{family:'Roboto'}} } } }" +
                              "});" +
                              "setTimeout(function(){ window.location.reload(); }, 60000);" + 
                              "</script>" +
                              "</body></html>";

                byte[] responseBytes = html.getBytes("UTF-8");
                t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                t.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = t.getResponseBody();
                os.write(responseBytes);
                os.close();
            } catch (Exception ex) {
                String error = "<html><body><h1>Server Crash Detected!</h1><pre>" + ex.toString() + "</pre></body></html>";
                for(StackTraceElement el : ex.getStackTrace()) error += el.toString() + "<br>";
                byte[] errBytes = error.getBytes("UTF-8");
                t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                t.sendResponseHeaders(500, errBytes.length);
                OutputStream os = t.getResponseBody();
                os.write(errBytes);
                os.close();
            }
        }
    }

    static class AddAlertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String body = scanner.hasNext() ? scanner.next() : "";
                
                String type = "Generic", location = "Unknown", extra = "0", username = "Operator";
                int severity = 1;

                String[] pairs = body.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length == 2) {
                        String key = kv[0];
                        String value = URLDecoder.decode(kv[1], "UTF-8").replace("+", " ");
                        if (key.equals("type")) type = value;
                        else if (key.equals("location")) location = value;
                        else if (key.equals("severity")) severity = Integer.parseInt(value);
                        else if (key.equals("extra")) extra = value;
                        else if (key.equals("username")) username = value;
                    }
                }

                double lat = 30.0 + new Random().nextDouble()*10;
                double lon = 60.0 + new Random().nextDouble()*10;
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                Disaster d = null;
                if (type.equals("Earthquake")) d = new Earthquake(location, severity, time, lat, lon, Double.parseDouble(extra));
                else if (type.equals("Flood")) d = new Flood(location, severity, time, lat, lon, Double.parseDouble(extra));
                else if (type.equals("Wildfire")) d = new Wildfire(location, severity, time, lat, lon, extra);
                else if (type.equals("Heatwave")) d = new Heatwave(location, severity, time, lat, lon, Double.parseDouble(extra));
                else if (type.equals("Landslide")) d = new Landslide(location, severity, time, lat, lon, extra);
                else d = new Disaster(location, severity);

                d.setWeatherTemp(fetchWeather(lat, lon));
                addAlertAndSave(d);

                t.getResponseHeaders().add("Location", "/dashboard?username=" + username);
                t.sendResponseHeaders(302, -1);
            }
        }
    }
}
