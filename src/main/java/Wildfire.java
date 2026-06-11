public class Wildfire extends Disaster {
    private String windDirection;

    public Wildfire(String location, int severityLevel, String windDirection) {
        super(location, severityLevel);
        this.windDirection = windDirection;
    }

    public Wildfire(String location, int severityLevel, String timestamp, double lat, double lon, String windDirection) {
        super(location, severityLevel, timestamp, lat, lon);
        this.windDirection = windDirection;
    }

    @Override
    public String getType() { return "Wildfire"; }

    @Override
    public String toCSV() {
        return getType() + "," + getLocation() + "," + getSeverityLevel() + "," + getTimestamp() + "," + getLat() + "," + getLon() + "," + windDirection;
    }

    @Override
    public String getEvacuationPlan() {
        if (getSeverityLevel() >= 3) {
            return "ACTION: Evacuate towards " + windDirection + " direction.";
        }
        return "ACTION: Stay indoors, close windows.";
    }

    @Override
    public String generateAlert() {
        String severityClass = getSeverityLevel() >= 3 ? "severity-high" : "severity-medium";
        String html = "<div class='alert-card alert-item " + severityClass + "' data-type='Wildfire' data-lat='" + getLat() + "' data-lon='" + getLon() + "'>" +
               "<div class='alert-header'><div class='alert-icon' style='background:rgba(249, 115, 22, 0.2); color:#f97316;'><i class='fa-solid fa-fire'></i></div>" +
               "<div><h3 class='alert-title'>Wildfire Event</h3><p class='alert-meta'>" + getLocation() + " | Wind: " + windDirection + " | Time: " + getTimestamp() + "</p>" +
               "<p class='alert-meta' style='color:#00e5ff; font-weight:600; font-size:0.9rem;'><i class='fa-solid fa-cloud-sun'></i> " + getWeatherTemp() + "</p></div></div>";
        
        String color = getSeverityLevel() >= 3 ? "red" : "generic";
        html += "<div class='action-box " + color + "'><i class='fa-solid fa-truck-fast'></i> " + getEvacuationPlan() + "</div>";
        html += "</div>";
        return html;
    }
}
