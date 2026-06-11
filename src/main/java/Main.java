import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Apna Name Enter Karein (User Name): ");
        String userName = scanner.nextLine();
        
        System.out.println("\n=== Welcome " + userName + " to the Real-Time Disaster Alert System ===\n");
        
        // Creating a list of the Base Class (Disaster)
        List<Disaster> activeDisasters = new ArrayList<>();
        
        // Polymorphism in action: storing subclasses (Earthquake, Flood) in a list of superclass type
        activeDisasters.add(new Earthquake("Karachi", 4, 6.5));
        activeDisasters.add(new Flood("Sindh Coastal Areas", 3, 2.5));
        activeDisasters.add(new Disaster("Lahore", 1)); // A generic minor disaster
        
        System.out.println("Processing Alerts...\n");
        
        // Iterating through the list and calling the overridden methods
        for (Disaster d : activeDisasters) {
            // Polymorphism: The correct generateAlert() method is called based on the object's actual type
            d.generateAlert();
            System.out.println("-------------------------------------------------");
        }
        
        System.out.println("\nSystem Update Complete.");
        
        scanner.close();
    }
}
