package net.not_thefirst.story_mode_clouds.config.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.screens.Screen;

public interface ConfigBackend {
    ConfigScreenBuilder createScreen(String title, Runnable saveAction);

    ConfigCategoryBuilder createCategory(String title, String tooltip);

    ConfigGroupBuilder createGroup(String title);

    BooleanOptionBuilder booleanOption(String title, String description, Supplier<Boolean> getter, Consumer<Boolean> setter);
    IntegerOptionBuilder integerOption(String title, String description, Supplier<Integer> getter, Consumer<Integer> setter);
    FloatOptionBuilder floatOption(String title, String description, Supplier<Float> getter, Consumer<Float> setter);
    StringOptionBuilder stringOption(String title, String description, Supplier<String> getter, Consumer<String> setter);
    <T extends Enum<T>> EnumOptionBuilder<T> enumOption(String title, String description, Supplier<T> getter, Consumer<T> setter);
    ColorOptionBuilder colorOption(String title, String description, Supplier<java.awt.Color> getter, Consumer<java.awt.Color> setter);

    ConfigActionBuilder createAction(String title, String description, Runnable action);

    interface ConfigScreenBuilder {
        ConfigScreenBuilder category(ConfigCategoryBuilder category);
        ConfigScreenBuilder category(dev.isxander.yacl3.api.ConfigCategory category);
        ConfigScreenBuilder category(String title, String tooltip, Consumer<ConfigCategoryBuilder> initializer);
        Screen build(Screen backScreen);
    }

    interface ConfigCategoryBuilder {
        ConfigCategoryBuilder name(String name);
        ConfigCategoryBuilder tooltip(String tooltip);
        ConfigCategoryBuilder option(ConfigOptionBuilder<?> option);
        ConfigCategoryBuilder group(ConfigGroupBuilder group);
        ConfigCategoryBuilder action(ConfigActionBuilder action);
        ConfigCategorySpec build();
    }

    interface ConfigGroupBuilder {
        ConfigGroupBuilder name(String name);
        ConfigGroupBuilder description(String description);
        ConfigGroupBuilder option(ConfigOptionBuilder<?> option);
        ConfigGroupBuilder action(ConfigActionBuilder action);
        ConfigGroupSpec build();
    }

    interface ConfigOptionBuilder<T> {
        ConfigOptionBuilder<T> name(String name);
        ConfigOptionBuilder<T> description(String description);
        ConfigOptionBuilder<T> binding(Supplier<T> getter, Consumer<T> setter);
        ConfigOptionSpec<T> build();
    }

    interface BooleanOptionBuilder extends ConfigOptionBuilder<Boolean> {}

    interface IntegerOptionBuilder extends ConfigOptionBuilder<Integer> {
        IntegerOptionBuilder range(int min, int max);
        IntegerOptionBuilder step(int step);
        IntegerOptionBuilder slider();
        IntegerOptionBuilder field();
    }

    interface FloatOptionBuilder extends ConfigOptionBuilder<Float> {
        FloatOptionBuilder range(float min, float max);
        FloatOptionBuilder step(float step);
        FloatOptionBuilder slider();
        FloatOptionBuilder field();
    }

    interface StringOptionBuilder extends ConfigOptionBuilder<String> {
        StringOptionBuilder dropDown(List<String> options);
    }

    interface EnumOptionBuilder<T extends Enum<T>> extends ConfigOptionBuilder<T> {
        EnumOptionBuilder<T> enumClass(Class<T> enumClass);
    }

    interface ColorOptionBuilder extends ConfigOptionBuilder<java.awt.Color> {}

    interface ConfigActionBuilder {
        ConfigActionBuilder name(String name);
        ConfigActionBuilder description(String description);
        ConfigActionBuilder action(Runnable action);
        ConfigActionSpec build();
    }

    final class ConfigCategorySpec {
        public final String name;
        public final String tooltip;
        public final List<ConfigGroupSpec> groups = new ArrayList<>();
        public final List<ConfigOptionSpec<?>> options = new ArrayList<>();
        public final List<ConfigActionSpec> actions = new ArrayList<>();
        public final Object backendNode;

        public ConfigCategorySpec(String name, String tooltip) {
            this(name, tooltip, null);
        }

        public ConfigCategorySpec(String name, String tooltip, Object backendNode) {
            this.name = name;
            this.tooltip = tooltip;
            this.backendNode = backendNode;
        }
    }

    final class ConfigGroupSpec {
        public final String name;
        public final List<ConfigOptionSpec<?>> options = new ArrayList<>();
        public final List<ConfigActionSpec> actions = new ArrayList<>();
        public final Object backendNode;

        public ConfigGroupSpec(String name) {
            this(name, null);
        }

        public ConfigGroupSpec(String name, Object backendNode) {
            this.name = name;
            this.backendNode = backendNode;
        }
    }

    final class ConfigOptionSpec<T> {
        public final String name;
        public final String description;
        public final Supplier<T> getter;
        public final Consumer<T> setter;
        public final Object backendNode;

        public ConfigOptionSpec(String name, String description, Supplier<T> getter, Consumer<T> setter) {
            this(name, description, getter, setter, null);
        }

        public ConfigOptionSpec(String name, String description, Supplier<T> getter, Consumer<T> setter, Object backendNode) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
            this.backendNode = backendNode;
        }
    }

    final class ConfigActionSpec {
        public final String name;
        public final String description;
        public final Runnable action;
        public final Object backendNode;

        public ConfigActionSpec(String name, String description, Runnable action) {
            this(name, description, action, null);
        }

        public ConfigActionSpec(String name, String description, Runnable action, Object backendNode) {
            this.name = name;
            this.description = description;
            this.action = action;
            this.backendNode = backendNode;
        }
    }

}
