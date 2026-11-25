import java.util.Map;

public final class Main {
    public static void main(String[] args) throws ReflectiveOperationException {
        Warehouse warehouse = new Warehouse();
        Warehouse.Product mugs = new Warehouse.Product("Кружка", 0.25);
        Warehouse.Product plates = new Warehouse.Product("Тарелка", 0.4);

        warehouse.addStock(mugs, 24);
        warehouse.addStock(plates, 10);
        warehouse.removeStock(mugs, 1);

        Warehouse.Shipment shipment = new Warehouse.Shipment(Map.of(mugs, 6, plates, 4));
        System.out.println(WarehouseDemo.run(warehouse, shipment, "Магазин"));
    }
}
