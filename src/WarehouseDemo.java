import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public final class WarehouseDemo {
    private WarehouseDemo() {}

    public static String run(Warehouse warehouse, Warehouse.Shipment shipment, String destination)
            throws ReflectiveOperationException {
        Objects.requireNonNull(warehouse, "Укажите склад");
        Objects.requireNonNull(shipment, "Укажите партию");
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Укажите получателя партии");
        }

        String before = describeStock(warehouse, shipment);
        String result = invokeRepeated(warehouse, shipment, destination.strip());
        String after = describeStock(warehouse, shipment);
        return String.join(System.lineSeparator(), "Остатки до отгрузки — " + before, result,
                "Остатки после отгрузки — " + after);
    }

    private static String describeStock(Warehouse warehouse, Warehouse.Shipment shipment) {
        return shipment.quantities().keySet().stream()
                .map(product -> "%s: %d шт.".formatted(product.name(), warehouse.stockOf(product)))
                .collect(Collectors.joining(", "));
    }

    static String invokeRepeated(Warehouse warehouse, Warehouse.Shipment shipment, String destination)
            throws ReflectiveOperationException {
        Object[] values = {shipment, destination};
        Method[] methods = warehouse.getClass().getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        StringJoiner results = new StringJoiner(System.lineSeparator());

        for (Method method : methods) {
            Repeat repeat = method.getAnnotation(Repeat.class);
            int modifiers = method.getModifiers();
            if (repeat == null || !(Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))) {
                continue;
            }

            int count = repeat.value();
            if (count < 0) {
                throw new IllegalArgumentException("Отрицательное число повторов для " + method.getName());
            }
            if (count == 0) {
                continue;
            }
            Object[] arguments = resolveArguments(method, values, results);
            method.setAccessible(true);

            for (int attempt = 0; attempt < count; attempt++) {
                Object result = method.invoke(warehouse, arguments);
                results.add("%s (%d/%d) — %s".formatted(method.getName(), attempt + 1, count, result));
            }
        }
        return results.toString();
    }

    private static Object[] resolveArguments(Method method, Object[] values, StringJoiner results)
            throws ReflectiveOperationException {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int index = 0; index < types.length; index++) {
            Class<?> type = types[index];
            Class<?> valueType = MethodType.methodType(type).wrap().returnType();
            for (Object value : values) {
                if (valueType.isInstance(value)) {
                    arguments[index] = value;
                    break;
                }
            }
            if (arguments[index] == null) {
                try {
                    arguments[index] = createArgument(type);
                } catch (ReflectiveOperationException exception) {
                    throw new ReflectiveOperationException(
                            "Не удалось создать аргумент типа %s для метода %s"
                                    .formatted(type.getTypeName(), method.getName()), exception);
                }
                results.add(("Аргумент типа %s для метода %s не найден среди переданных значений. "
                        + "Будет использовано значение по умолчанию.")
                        .formatted(type.getTypeName(), method.getName()));
            }
        }
        return arguments;
    }

    private static Object createArgument(Class<?> type) throws ReflectiveOperationException {
        Class<?> primitiveType = MethodType.methodType(type).unwrap().returnType();
        if (primitiveType.isPrimitive() && primitiveType != void.class) {
            return Array.get(Array.newInstance(primitiveType, 1), 0);
        }
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        return type.getDeclaredConstructor().newInstance();
    }
}
