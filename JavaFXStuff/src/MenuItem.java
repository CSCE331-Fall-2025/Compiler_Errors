/**
 * Represents a menu item 
 */
public class MenuItem {
    private String name;
    private double price;
    private String ingredients;
    private String category;
    
    /**
     * Standard constructor for a menu item
     * @param name name of menu item
     * @param price price of item
     * @param ingredients string formatted list of ingredients (ingr, ingr, etc)
     * @param category category of menu item (entree, drink, etc)
     */
    public MenuItem(String name, double price, String ingredients, String category) {
        this.name = name;
        this.price = price;
        this.ingredients = ingredients;
        this.category = category;
    }
    
    /**
     * Default constructor for entrees. Does not require ingredients
     * @param name name of menu item
     * @param price price of item
     */
    public MenuItem(String name, double price) {
        this.name = name;
        this.price = price;
        this.ingredients = "";
        this.category = "entree";
    }
    
    /**
     * Gets name of menu item
     * @return String
     */
    public String getName() { return name; }

    /**
     * Sets name of menu item
     * @param name String
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Gets price of menu item
     * @return double
     */
    public double getPrice() { return price; }

    /**
     * Sets price of menu item
     * @param price double
     */
    public void setPrice(double price) { this.price = price; }
    
    /**
     * Gets ingredients of menu item
     * @return String
     */
    public String getIngredients() { return ingredients; }

    /**
     * Sets ingredients of menu item
     * @param ingredients String (ingr,ingr,ingr)
     */
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    
    /**
     * Gets category of menu item
     * @return String
     */
    public String getCategory() { return category; }

    /**
     * Sets category of menu item
     * @param category String
     */
    public void setCategory(String category) { this.category = category; }
    
    /**
     * Returns name and price of an item
     * @return String
     */
    @Override
    public String toString() {
        return name + " - $" + String.format("%.2f", price);
    }
    
    /**
     * Equality function for menu items
     * @return True if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MenuItem other = (MenuItem) obj;
        return name.equals(other.name);
    }
    
    /**
     * Returns hashcode of menu item's name
     * @return int
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}