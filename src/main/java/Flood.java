public class Flood extends Disaster {
    private double waterLevelIncrease;

    public Flood(String location, int severityLevel, double waterLevelIncrease) {
        super(location, severityLevel);
        this.waterLevelIncrease = waterLevelIncrease;
    }

    public Flood(String location, int severityLevel, String timestamp, double lat, double lon, double waterLevelIncrease) {
        super(location, severityLevel, timestamp, lat, lon);
        this.waterLevelIncrease = waterLevelIncrease;
    }

    @Override
    public String getType() { return "Flood"; }

    @Override
    public String toCSV() {
        return getType() + "," + getLocation() + "," + getSeverityLevel() + "," + getTimestamp() + "," + getLat() + "," + getLon() + "," + waterLevelIncrease;
    }

    @Override
    public String getEvacuationPlan() {
        if (getSeverityLevel() >= 3) {
            return "ACTION: Move to higher ground immediately! Avoid driving.";
        }
        return "ACTION: Prepare to evacuate.";
    }

    @Override
    public String generateAlert() {
        String severityClass = getSeverityLevel() >= 3 ? "severity-high" : "severity-medium";
        String html = "<div class='alert-card alert-item " + severityClass + "' data-type='Flood' data-lat='" + getLat() + "' data-lon='" + getLon() + "'>" +
               "<div class='alert-header'><div class='alert-icon' style='background:rgba(59, 130, 246, 0.2); color:#3b82f6;'><i class='fa-solid fa-water'></i></div>" +
               "<div><h3 class='alert-title'>Flood Warning</h3><p class='alert-meta'>" + getLocation() + " | Level: +" + waterLevelIncrease + "m | Time: " + getTimestamp() + "</p>" +
               "<p class='alert-meta' style='color:#00e5ff; font-weight:600; font-size:0.9rem;'><i class='fa-solid fa-cloud-sun'></i> " + getWeatherTemp() + "</p></div></div>";
        
        String color = getSeverityLevel() >= 3 ? "red" : "blue";
        html += "<div class='action-box " + color + "'><i class='fa-solid fa-arrow-up'></i> " + getEvacuationPlan() + "</div>";
        html += "</div>";
        return html;
    }
}
