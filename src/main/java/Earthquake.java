public class Earthquake extends Disaster {
    private double magnitude;

    public Earthquake(String location, int severityLevel, double magnitude) {
        super(location, severityLevel);
        this.magnitude = magnitude;
    }
    
    public Earthquake(String location, int severityLevel, String timestamp, double lat, double lon, double magnitude) {
        super(location, severityLevel, timestamp, lat, lon);
        this.magnitude = magnitude;
    }

    @Override
    public String getType() { return "Earthquake"; }

    @Override
    public String toCSV() {
        return getType() + "," + getLocation() + "," + getSeverityLevel() + "," + getTimestamp() + "," + getLat() + "," + getLon() + "," + magnitude;
    }

    @Override
    public String getEvacuationPlan() {
        if (getSeverityLevel() >= 4) {
            return "ACTION: Evacuate immediately and find open ground! Avoid buildings.";
        }
        return "ACTION: Drop, Cover, and Hold on.";
    }

    @Override
    public String generateAlert() {
        String severityClass = getSeverityLevel() >= 4 ? "severity-high" : "severity-medium";
        String html = "<div class='alert-card alert-item " + severityClass + "' data-type='Earthquake' data-lat='" + getLat() + "' data-lon='" + getLon() + "'>" +
               "<div class='alert-header'><div class='alert-icon'><i class='fa-solid fa-house-crack'></i></div>" +
               "<div><h3 class='alert-title'>Earthquake</h3><p class='alert-meta'>" + getLocation() + " | Mag: " + magnitude + " | Time: " + getTimestamp() + "</p>" +
               "<p class='alert-meta' style='color:#00e5ff; font-weight:600; font-size:0.9rem;'><i class='fa-solid fa-cloud-sun'></i> " + getWeatherTemp() + "</p></div></div>";
        
        String color = getSeverityLevel() >= 4 ? "red" : "blue";
        html += "<div class='action-box " + color + "'><i class='fa-solid fa-person-running'></i> " + getEvacuationPlan() + "</div>";
        html += "</div>";
        return html;
    }
}
