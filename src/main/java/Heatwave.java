public class Heatwave extends Disaster {
    private double temperature;

    public Heatwave(String location, int severityLevel, double temperature) {
        super(location, severityLevel);
        this.temperature = temperature;
    }

    public Heatwave(String location, int severityLevel, String timestamp, double lat, double lon, double temperature) {
        super(location, severityLevel, timestamp, lat, lon);
        this.temperature = temperature;
    }

    @Override
    public String getType() { return "Heatwave"; }

    @Override
    public String toCSV() {
        return getType() + "," + getLocation() + "," + getSeverityLevel() + "," + getTimestamp() + "," + getLat() + "," + getLon() + "," + temperature;
    }

    @Override
    public String getEvacuationPlan() {
        if (getSeverityLevel() >= 4) {
            return "ACTION: Severe Heat Alert! Stay indoors, stay hydrated, avoid direct sunlight.";
        }
        return "ACTION: Drink plenty of water and wear light clothes.";
    }

    @Override
    public String generateAlert() {
        String severityClass = getSeverityLevel() >= 4 ? "severity-high" : "severity-medium";
        String html = "<div class='alert-card alert-item " + severityClass + "' data-type='Heatwave' data-lat='" + getLat() + "' data-lon='" + getLon() + "'>" +
               "<div class='alert-header'><div class='alert-icon' style='background:rgba(239, 68, 68, 0.2); color:#ef4444;'><i class='fa-solid fa-temperature-high'></i></div>" +
               "<div><h3 class='alert-title'>Heatwave</h3><p class='alert-meta'>" + getLocation() + " | Temp: " + temperature + "°C | Time: " + getTimestamp() + "</p>" +
               "<p class='alert-meta' style='color:#00e5ff; font-weight:600; font-size:0.9rem;'><i class='fa-solid fa-cloud-sun'></i> " + getWeatherTemp() + "</p></div></div>";
        
        String color = getSeverityLevel() >= 4 ? "red" : "blue";
        html += "<div class='action-box " + color + "'><i class='fa-solid fa-glass-water'></i> " + getEvacuationPlan() + "</div>";
        html += "</div>";
        return html;
    }
}
