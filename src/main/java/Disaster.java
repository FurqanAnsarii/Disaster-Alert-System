import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Disaster implements EmergencyProtocol {
    private String location;
    private int severityLevel;
    private String timestamp;
    private double lat;
    private double lon;
    private String weatherTemp = "Unknown";

    // Manual Creation (Defaults to Pakistan coords if not specified)
    public Disaster(String location, int severityLevel) {
        this.location = location;
        this.severityLevel = severityLevel;
        this.lat = 30.3753; // Default PK
        this.lon = 69.3451;
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.timestamp = now.format(formatter);
    }
    
    // Loaded from File or API
    public Disaster(String location, int severityLevel, String timestamp, double lat, double lon) {
        this.location = location;
        this.severityLevel = severityLevel;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
    }

    public String getLocation() { return location; }
    public int getSeverityLevel() { return severityLevel; }
    public String getTimestamp() { return timestamp; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getWeatherTemp() { return weatherTemp; }
    public void setWeatherTemp(String temp) { this.weatherTemp = temp; }

    public String getType() { return "Generic Disaster"; }
    
    public String toCSV() {
        return getType() + "," + location + "," + severityLevel + "," + timestamp + "," + lat + "," + lon + ",None";
    }

    @Override
    public String getEvacuationPlan() {
        return "Follow general emergency guidelines and stay tuned for updates.";
    }

    @Override
    public String generateAlert() {
        return "<div class='alert-card alert-item' data-type='Generic' data-lat='" + lat + "' data-lon='" + lon + "'>" +
               "<div class='alert-header'><div class='alert-icon generic'><i class='fa-solid fa-triangle-exclamation'></i></div>" +
               "<div><h3 class='alert-title'>General Alert</h3><p class='alert-meta'>" + location + " | Time: " + timestamp + "</p></div></div>" +
               "<div class='action-box generic'><i class='fa-solid fa-shield'></i> " + getEvacuationPlan() + "</div>" +
               "</div>";
    }
}
