public class Tsunami extends Disaster {
    private double waveHeight;

    public Tsunami(String location, int severityLevel, double waveHeight) {
        super(location, severityLevel);
        this.waveHeight = waveHeight;
    }

    public Tsunami(String location, int severityLevel, String timestamp, double lat, double lon, double waveHeight) {
        super(location, severityLevel, timestamp, lat, lon);
        this.waveHeight = waveHeight;
    }

    @Override
    public String getType() { return "Tsunami"; }

    @Override
    public String toCSV() {
        return getType() + "," + getLocation() + "," + getSeverityLevel() + "," + getTimestamp() + "," + getLat() + "," + getLon() + "," + waveHeight;
    }

    @Override
    public String getEvacuationPlan() {
        if (getSeverityLevel() >= 4) {
            return "ACTION: Tsunami warning! Move inland immediately.";
        }
        return "ACTION: Tsunami watch. Stay away from the beach.";
    }

    @Override
    public String generateAlert() {
        String severityClass = getSeverityLevel() >= 4 ? "severity-high" : "severity-medium";
        String html = "<div class='alert-card alert-item " + severityClass + "' data-type='Tsunami' data-lat='" + getLat() + "' data-lon='" + getLon() + "'>" +
               "<div class='alert-header'><div class='alert-icon' style='background:rgba(6, 182, 212, 0.2); color:#06b6d4;'><i class='fa-solid fa-water'></i><i class='fa-solid fa-arrow-trend-up'></i></div>" +
               "<div><h3 class='alert-title'>Tsunami</h3><p class='alert-meta'>" + getLocation() + " | Wave: " + waveHeight + "m | Time: " + getTimestamp() + "</p></div></div>";
        
        String color = getSeverityLevel() >= 4 ? "red" : "blue";
        html += "<div class='action-box " + color + "'><i class='fa-solid fa-person-hiking'></i> " + getEvacuationPlan() + "</div>";
        html += "</div>";
        return html;
    }
}
