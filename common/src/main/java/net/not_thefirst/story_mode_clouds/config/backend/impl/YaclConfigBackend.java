package net.not_thefirst.story_mode_clouds.config.backend.impl;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.not_thefirst.story_mode_clouds.config.backend.ConfigBackend;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.awt.Color;

public final class YaclConfigBackend implements ConfigBackend {
    YaclConfigBackend() {}

    public static YaclConfigBackend create() {
        return new YaclConfigBackend();
    }

    @Override
    public ConfigScreenBuilder createScreen(String title, Runnable saveAction) {
        return new YaclScreenBuilder(title, saveAction);
    }

    @Override
    public ConfigCategoryBuilder createCategory(String title, String tooltip) {
        return new YaclCategoryBuilder(title, tooltip);
    }

    @Override
    public ConfigGroupBuilder createGroup(String title) {
        return new YaclGroupBuilder(title);
    }

    @Override
    public BooleanOptionBuilder booleanOption(String title, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new YaclBooleanOptionBuilder(title, description, getter, setter);
    }

    @Override
    public IntegerOptionBuilder integerOption(String title, String description, Supplier<Integer> getter, Consumer<Integer> setter) {
        return new YaclIntegerOptionBuilder(title, description, getter, setter);
    }

    @Override
    public FloatOptionBuilder floatOption(String title, String description, Supplier<Float> getter, Consumer<Float> setter) {
        return new YaclFloatOptionBuilder(title, description, getter, setter);
    }

    @Override
    public StringOptionBuilder stringOption(String title, String description, Supplier<String> getter, Consumer<String> setter) {
        return new YaclStringOptionBuilder(title, description, getter, setter);
    }

    @Override
    public <T extends Enum<T>> EnumOptionBuilder<T> enumOption(String title, String description, Supplier<T> getter, Consumer<T> setter) {
        return new YaclEnumOptionBuilder<>(title, description, getter, setter);
    }

    @Override
    public ColorOptionBuilder colorOption(String title, String description, Supplier<Color> getter, Consumer<Color> setter) {
        return new YaclColorOptionBuilder(title, description, getter, setter);
    }

    @Override
    public ConfigActionBuilder createAction(String title, String description, Runnable action) {
        return new YaclActionBuilder(title, description, action);
    }

    private static final class YaclScreenBuilder implements ConfigScreenBuilder {
        private final String title;
        private final Runnable saveAction;
        private final List<ConfigCategoryBuilder> categories = new ArrayList<>();

        private YaclScreenBuilder(String title, Runnable saveAction) {
            this.title = title;
            this.saveAction = saveAction;
        }

        @Override
        public ConfigScreenBuilder category(ConfigCategoryBuilder category) {
            categories.add(category);
            return this;
        }

        @Override
        public ConfigScreenBuilder category(ConfigCategory category) {
            categories.add(new ConfigCategoryBuilder() {
                @Override
                public ConfigCategoryBuilder name(String name) {
                    return this;
                }

                @Override
                public ConfigCategoryBuilder tooltip(String tooltip) {
                    return this;
                }

                @Override
                public ConfigCategoryBuilder option(ConfigOptionBuilder<?> option) {
                    return this;
                }

                @Override
                public ConfigCategoryBuilder group(ConfigGroupBuilder group) {
                    return this;
                }

                @Override
                public ConfigCategoryBuilder action(ConfigActionBuilder action) {
                    return this;
                }

                @Override
                public ConfigCategorySpec build() {
                    return new ConfigCategorySpec(category.name().toString(), null, category);
                }
            });
            return this;
        }

        @Override
        public ConfigScreenBuilder category(String title, String tooltip, Consumer<ConfigCategoryBuilder> initializer) {
            ConfigCategoryBuilder builder = new YaclCategoryBuilder(title, tooltip);
            initializer.accept(builder);
            return category(builder);
        }

        @Override
        public Screen build(Screen backScreen) {
            var builder = YetAnotherConfigLib.createBuilder()
                .title(resolveText(title))
                .save(() -> {
                    if (saveAction != null) {
                        saveAction.run();
                    }
                });

            for (ConfigCategoryBuilder categoryBuilder : categories) {
                ConfigCategorySpec spec = categoryBuilder.build();
                if (spec.backendNode instanceof ConfigCategory) {
                    builder.category((ConfigCategory) spec.backendNode);
                }
            }

            return builder.build().generateScreen(backScreen);
        }
    }

    private static final class YaclCategoryBuilder implements ConfigCategoryBuilder {
        private String name;
        private String tooltip;
        private final List<ConfigOptionBuilder<?>> options = new ArrayList<>();
        private final List<ConfigGroupBuilder> groups = new ArrayList<>();
        private final List<ConfigActionBuilder> actions = new ArrayList<>();

        private YaclCategoryBuilder(String name, String tooltip) {
            this.name = name;
            this.tooltip = tooltip;
        }

        @Override
        public ConfigCategoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigCategoryBuilder tooltip(String tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        @Override
        public ConfigCategoryBuilder option(ConfigOptionBuilder<?> option) {
            options.add(option);
            return this;
        }

        @Override
        public ConfigCategoryBuilder group(ConfigGroupBuilder group) {
            groups.add(group);
            return this;
        }

        @Override
        public ConfigCategoryBuilder action(ConfigActionBuilder action) {
            actions.add(action);
            return this;
        }

        @Override
        public ConfigCategorySpec build() {
            var builder = ConfigCategory.createBuilder()
                .name(resolveText(name));

            if (tooltip != null && !tooltip.isBlank()) {
                builder.tooltip(resolveText(tooltip));
            }

            for (ConfigOptionBuilder<?> optionBuilder : options) {
                ConfigOptionSpec<?> spec = optionBuilder.build();
                if (spec.backendNode instanceof Option<?>) {
                    builder.option((Option<?>) spec.backendNode);
                }
            }

            for (ConfigGroupBuilder groupBuilder : groups) {
                ConfigGroupSpec spec = groupBuilder.build();
                if (spec.backendNode instanceof OptionGroup) {
                    builder.group((OptionGroup) spec.backendNode);
                }
            }

            for (ConfigActionBuilder actionBuilder : actions) {
                ConfigActionSpec spec = actionBuilder.build();
                if (spec.backendNode instanceof Option<?>) {
                    builder.option((Option<?>) spec.backendNode);
                }
            }

            return new ConfigCategorySpec(name, tooltip, builder.build());
        }
    }

    private static final class YaclGroupBuilder implements ConfigGroupBuilder {
        private String name;
        private String description;
        private final List<ConfigOptionBuilder<?>> options = new ArrayList<>();
        private final List<ConfigActionBuilder> actions = new ArrayList<>();

        private YaclGroupBuilder(String name) {
            this.name = name;
        }

        @Override
        public ConfigGroupBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigGroupBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigGroupBuilder option(ConfigOptionBuilder<?> option) {
            options.add(option);
            return this;
        }

        @Override
        public ConfigGroupBuilder action(ConfigActionBuilder action) {
            actions.add(action);
            return this;
        }

        @Override
        public ConfigGroupSpec build() {
            var builder = OptionGroup.createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            for (ConfigOptionBuilder<?> optionBuilder : options) {
                ConfigOptionSpec<?> spec = optionBuilder.build();
                if (spec.backendNode instanceof Option<?>) {
                    builder.option((Option<?>) spec.backendNode);
                }
            }

            for (ConfigActionBuilder actionBuilder : actions) {
                ConfigActionSpec spec = actionBuilder.build();
                if (spec.backendNode instanceof Option<?>) {
                    builder.option((Option<?>) spec.backendNode);
                }
            }

            return new ConfigGroupSpec(name, builder.build());
        }
    }

    private static final class YaclBooleanOptionBuilder implements BooleanOptionBuilder {
        private String name;
        private String description;
        private Supplier<Boolean> getter;
        private Consumer<Boolean> setter;

        private YaclBooleanOptionBuilder(String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public ConfigOptionBuilder<Boolean> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Boolean> description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Boolean> binding(Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        @Override
        public ConfigOptionSpec<Boolean> build() {
            Option.Builder<Boolean> builder = Option.<Boolean>createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            Boolean initialValue = getter != null ? getter.get() : null;
            builder.binding(initialValue, getter, setter);
            builder.controller(BooleanControllerBuilder::create);

            return new ConfigOptionSpec<>(name, description, getter, setter, builder.build());
        }
    }

    private static final class YaclIntegerOptionBuilder implements IntegerOptionBuilder {
        private String name;
        private String description;
        private Supplier<Integer> getter;
        private Consumer<Integer> setter;
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;
        private int step = 1;
        private boolean slider = false;

        private YaclIntegerOptionBuilder(String name, String description, Supplier<Integer> getter, Consumer<Integer> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public IntegerOptionBuilder range(int min, int max) {
            this.min = min;
            this.max = max;
            return this;
        }

        @Override
        public IntegerOptionBuilder step(int step) {
            this.step = step;
            return this;
        }

        @Override
        public IntegerOptionBuilder slider() {
            this.slider = true;
            return this;
        }

        @Override
        public IntegerOptionBuilder field() {
            this.slider = false;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Integer> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Integer> description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Integer> binding(Supplier<Integer> getter, Consumer<Integer> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        @Override
        public ConfigOptionSpec<Integer> build() {
            Option.Builder<Integer> builder = Option.<Integer>createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            Integer initialValue = getter != null ? getter.get() : null;
            builder.binding(initialValue, getter, setter);
            builder.controller(opt -> {
                if (slider) {
                    return IntegerSliderControllerBuilder.create(opt).range(min, max).step(step);
                }
                return IntegerFieldControllerBuilder.create(opt).range(min, max);
            });

            return new ConfigOptionSpec<>(name, description, getter, setter, builder.build());
        }
    }

    private static final class YaclFloatOptionBuilder implements FloatOptionBuilder {
        private String name;
        private String description;
        private Supplier<Float> getter;
        private Consumer<Float> setter;
        private float min = Float.MIN_VALUE;
        private float max = Float.MAX_VALUE;
        private float step = 0.01f;
        private boolean slider = false;

        private YaclFloatOptionBuilder(String name, String description, Supplier<Float> getter, Consumer<Float> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public FloatOptionBuilder range(float min, float max) {
            this.min = min;
            this.max = max;
            return this;
        }

        @Override
        public FloatOptionBuilder step(float step) {
            this.step = step;
            return this;
        }

        @Override
        public FloatOptionBuilder slider() {
            this.slider = true;
            return this;
        }

        @Override
        public FloatOptionBuilder field() {
            this.slider = false;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Float> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Float> description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Float> binding(Supplier<Float> getter, Consumer<Float> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        @Override
        public ConfigOptionSpec<Float> build() {
            Option.Builder<Float> builder = Option.<Float>createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            Float initialValue = getter != null ? getter.get() : null;
            builder.binding(initialValue, getter, setter);
            builder.controller(opt -> {
                if (slider) {
                    return FloatSliderControllerBuilder.create(opt).range(min, max).step(step);
                }
                return FloatFieldControllerBuilder.create(opt).range(min, max);
            });

            return new ConfigOptionSpec<>(name, description, getter, setter, builder.build());
        }
    }

    private static final class YaclStringOptionBuilder implements StringOptionBuilder {
        private String name;
        private String description;
        private Supplier<String> getter;
        private Consumer<String> setter;
        private List<String> dropDownOptions;

        private YaclStringOptionBuilder(String name, String description, Supplier<String> getter, Consumer<String> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
            this.dropDownOptions = null;
        }

        @Override
        public ConfigOptionBuilder<String> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigOptionBuilder<String> description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigOptionBuilder<String> binding(Supplier<String> getter, Consumer<String> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        @Override
        public StringOptionBuilder dropDown(List<String> options) {
            this.dropDownOptions = options;
            return this;
        }

        @Override
        public ConfigOptionSpec<String> build() {
            Option.Builder<String> builder = Option.<String>createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            String initialValue = getter != null ? getter.get() : null;
            builder.binding(initialValue, getter, setter);
            builder.controller(opt -> {
                if (dropDownOptions != null) {
                    return DropdownStringControllerBuilder.create(opt)
                        .values(dropDownOptions)
                        .allowAnyValue(false)
                        .allowEmptyValue(false);
                }
                return StringControllerBuilder.create(opt);
            });

            return new ConfigOptionSpec<>(name, description, getter, setter, builder.build());
        }
    }

    private static final class YaclEnumOptionBuilder<T extends Enum<T>> implements EnumOptionBuilder<T> {
        private String name;
        private String description;
        private Supplier<T> getter;
        private Consumer<T> setter;
        private Class<T> enumClass;

        private YaclEnumOptionBuilder(String name, String description, Supplier<T> getter, Consumer<T> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public EnumOptionBuilder<T> enumClass(Class<T> enumClass) {
            this.enumClass = enumClass;
            return this;
        }

        @Override
        public ConfigOptionBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigOptionBuilder<T> description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigOptionBuilder<T> binding(Supplier<T> getter, Consumer<T> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ConfigOptionSpec<T> build() {
            Option.Builder<T> builder = Option.<T>createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            T initialValue = getter != null ? getter.get() : null;
            builder.binding(initialValue, getter, setter);
            builder.controller(opt -> {
                if (enumClass != null) {
                    Option<T> typedOption = (Option<T>) opt;
                    return EnumControllerBuilder.create(typedOption).enumClass((Class) enumClass);
                }
                Option<String> stringOption = (Option<String>) (Option<?>) opt;
                return (ControllerBuilder<T>) (Object) StringControllerBuilder.create(stringOption);
            });

            return new ConfigOptionSpec<>(name, description, getter, setter, builder.build());
        }
    }

    private static final class YaclColorOptionBuilder implements ColorOptionBuilder {
        private String name;
        private String description;
        private Supplier<Color> getter;
        private Consumer<Color> setter;

        private YaclColorOptionBuilder(String name, String description, Supplier<Color> getter, Consumer<Color> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public ConfigOptionBuilder<Color> name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Color> description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigOptionBuilder<Color> binding(Supplier<Color> getter, Consumer<Color> setter) {
            this.getter = getter;
            this.setter = setter;
            return this;
        }

        @Override
        public ConfigOptionSpec<Color> build() {
            Option.Builder<Color> builder = Option.<Color>createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            Color initialValue = getter != null ? getter.get() : null;
            builder.binding(initialValue, getter, setter);
            builder.controller(ColorControllerBuilder::create);

            return new ConfigOptionSpec<>(name, description, getter, setter, builder.build());
        }
    }

    private static final class YaclActionBuilder implements ConfigActionBuilder {
        private String name;
        private String description;
        private Runnable action;

        private YaclActionBuilder(String name, String description, Runnable action) {
            this.name = name;
            this.description = description;
            this.action = action;
        }

        @Override
        public ConfigActionBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ConfigActionBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ConfigActionBuilder action(Runnable action) {
            this.action = action;
            return this;
        }

        @Override
        public ConfigActionSpec build() {
            ButtonOption.Builder builder = ButtonOption.createBuilder()
                .name(resolveText(name));

            if (description != null && !description.isBlank()) {
                builder.description(OptionDescription.of(resolveText(description)));
            }

            builder.action((yacl, btn) -> {
                if (action != null) {
                    action.run();
                }
            });

            return new ConfigActionSpec(name, description, action, builder.build());
        }
    }

    // the snake case special
    private static final Pattern PREFIX = Pattern.compile("^[a-zA-Z0-9]+(?:_[a-zA-Z0-9]+)*\\.");

    private static Component resolveText(String value) {
        if (value == null || value.isBlank()) {
            return ComponentWrapper.literal("");
        }

        if (PREFIX.matcher(value).find()) {
            return ComponentWrapper.translatable(value);
        }

        return ComponentWrapper.literal(value);
    }
}
