public class Landslide extends Disaster {
    private String roadStatus;

    public Landslide(String location, int severityLevel, String roadStatus) {
        super(location, severityLevel);
        this.roadStatus = roadStatus;
    }

    public Landslide(String location, int severityLevel, String timestamp, double lat, double lon, String roadStatus) {
        super(location, severityLevel, timestamp, lat, lon);
        this.roadStatus = roadStatus;
    }

    @Override
    public String getType() { return "Landslide"; }

    @Override
    public String toCSV() {
        return getType() + "," + getLocation() + "," + getSeverityLevel() + "," + getTimestamp() + "," + getLat() + "," + getLon() + "," + roadStatus;
    }

    @Override
    public String getEvacuationPlan() {
        if (getSeverityLevel() >= 3) {
            return "ACTION: Road is " + roadStatus + ". Avoid traveling to mountainous areas.";
        }
        return "ACTION: Drive carefully, beware of falling rocks.";
    }

    @Override
    public String generateAlert() {
        String severityClass = getSeverityLevel() >= 3 ? "severity-high" : "severity-medium";
        String html = "<div class='alert-card alert-item " + severityClass + "' data-type='Landslide' data-lat='" + getLat() + "' data-lon='" + getLon() + "'>" +
               "<div class='alert-header'><div class='alert-icon' style='background:rgba(139, 69, 19, 0.2); color:#8b4513;'><i class='fa-solid fa-hill-rockslide'></i></div>" +
               "<div><h3 class='alert-title'>Landslide</h3><p class='alert-meta'>" + getLocation() + " | Road: " + roadStatus + " | Time: " + getTimestamp() + "</p>" +
               "<p class='alert-meta' style='color:#00e5ff; font-weight:600; font-size:0.9rem;'><i class='fa-solid fa-cloud-sun'></i> " + getWeatherTemp() + "</p></div></div>";
        
        String color = getSeverityLevel() >= 3 ? "red" : "blue";
        html += "<div class='action-box " + color + "'><i class='fa-solid fa-triangle-exclamation'></i> " + getEvacuationPlan() + "</div>";
        html += "</div>";
        return html;
    }
}
