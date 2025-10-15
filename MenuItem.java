package src;

public class MenuItem {
    private String name;
    private double price;
    private String ingredients;
    private String category;
    
    public MenuItem(String name, double price, String ingredients, String category) {
        this.name = name;
        this.price = price;
        this.ingredients = ingredients;
        this.category = category;
    }
    
    public MenuItem(String name, double price) {
        this.name = name;
        this.price = price;
        this.ingredients = "";
        this.category = "entree";
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    @Override
    public String toString() {
        return name + " - $" + String.format("%.2f", price);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MenuItem other = (MenuItem) obj;
        return name.equals(other.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}