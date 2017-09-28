package model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String name;
    private String family;
    private LocalDate birthday;
    private Map<Component, Permission> permissions;

    public User() {
    }

    public User(String name, String family, LocalDate birthday) {
        this.name = name;
        this.family = family;
        this.birthday = birthday;
        Component[] allComponents = Component.values();
        permissions = new HashMap<>(allComponents.length);
        for (Component component:allComponents) {
            permissions.put(component, Permission.NONE);
        }
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public Permission getPermission(Component component) {
        return permissions.get(component);
    }

    public Permission setPermission(Component component, Permission permission) {
        return permissions.put(component, permission);
    }
}
