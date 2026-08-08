package net.not_thefirst.story_mode_clouds.config.screens;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ComponentWrapper;

public final class YACLUtils {
    private YACLUtils() {}

    public static void addIntegerOption(
        ConfigCategory.Builder category, 
        String title, 
        String desc, 
        Integer current, 
        Integer min, 
        Integer max, 
        Supplier<Integer> getter, 
        Consumer<Integer> setter) {

        category.option(Option.<Integer>createBuilder()
            .name(ComponentWrapper.translatable(title))
            .description(OptionDescription.of(ComponentWrapper.translatable(desc)))
            .binding(current, getter, setter)
            .controller(opt -> IntegerFieldControllerBuilder.create(opt).range(min, max))
            .build());
    }

    public static void addIntegerOption(
        ConfigCategory.Builder category, 
        String title, 
        String desc, 
        Integer current,
        Supplier<Integer> getter, 
        Consumer<Integer> setter) {
        addIntegerOption(category, title, desc, current, Integer.MIN_VALUE, Integer.MAX_VALUE, getter, setter);
    }

    public static void addFloatOption(
        ConfigCategory.Builder category, 
        String title, 
        String desc, 
        Float current, 
        Float min, 
        Float max, 
        Supplier<Float> getter, 
        Consumer<Float> setter) {

        category.option(Option.<Float>createBuilder()
            .name(ComponentWrapper.translatable(title))
            .description(OptionDescription.of(ComponentWrapper.translatable(desc)))
            .binding(current, getter, setter)
            .controller(opt -> FloatFieldControllerBuilder.create(opt).range(min, max))
            .build());
    }

    public static void addFloatOption(
        ConfigCategory.Builder category, 
        String title, 
        String desc, 
        Float current,
        Supplier<Float> getter, 
        Consumer<Float> setter) {
        addFloatOption(category, title, desc, current, Float.MIN_VALUE, Float.MAX_VALUE, getter, setter);
    }

    public static void addColorOption(
        ConfigCategory.Builder category, 
        String title, 
        String desc, 
        Integer current,
        Supplier<Integer> getter, 
        Consumer<Integer> setter) {

        category.option(Option.<Color>createBuilder()
            .name(ComponentWrapper.translatable(title))
            .description(OptionDescription.of(ComponentWrapper.translatable(desc)))
            .binding(
                new Color(getter.get()),
                () -> new Color(getter.get()),
                v -> setter.accept(v.getRGB()))
            .controller(ColorControllerBuilder::create)
            .build());
    }

    public static void addStringOption(
        ConfigCategory.Builder category, 
        String title, 
        String desc, 
        String current,
        Supplier<String> getter, 
        Consumer<String> setter) {

        category.option(Option.<String>createBuilder()
            .name(ComponentWrapper.translatable(title))
            .description(OptionDescription.of(ComponentWrapper.translatable(desc)))
            .binding(current, getter, setter)
            .controller(StringControllerBuilder::create)
            .build());
    }

}
