package bikerboys.pivot.screen;

import bikerboys.pivot.Pivot;
import bikerboys.pivot.PivotClient;
import bikerboys.pivot.Scheduler;
import bikerboys.pivot.networking.packets.*;
import bikerboys.pivot.util.Util;
import com.google.common.collect.ImmutableList;
import com.mojang.math.Transformation;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static bikerboys.pivot.util.Util.eulerDegreesToQuaternion;
import static bikerboys.pivot.util.Util.quaternionToEulerDegrees;

public class BlockDisplayEntityEditScreen extends BaseOwoScreen<FlowLayout> {
    private ItemDisplayContext currentContext = ItemDisplayContext.NONE;
    private int tick = 0;
    private boolean suppressUpdates = false;
    private Display displayEntity;

    private ScrollContainer<UIComponent> blockStateScroll = null;
    private FlowLayout blockStateFlow = null;

    // Euler sliders
    private final DiscreteSliderComponent rotXSlider = UIComponents.discreteSlider(Sizing.fixed(140), -180f, 180f).decimalPlaces(2);
    private final DiscreteSliderComponent rotYSlider = UIComponents.discreteSlider(Sizing.fixed(140), -180f, 180f).decimalPlaces(2);
    private final DiscreteSliderComponent rotZSlider = UIComponents.discreteSlider(Sizing.fixed(140), -180f, 180f).decimalPlaces(2);

    private final DiscreteSliderComponent scaleXSlider = UIComponents.discreteSlider(Sizing.fixed(140), 0f, 10f).decimalPlaces(2);
    private final DiscreteSliderComponent scaleYSlider = UIComponents.discreteSlider(Sizing.fixed(140), 0f, 10f).decimalPlaces(2);
    private final DiscreteSliderComponent scaleZSlider = UIComponents.discreteSlider(Sizing.fixed(140), 0f, 10f).decimalPlaces(2);

    private final TextBoxComponent positionXbox = UIComponents.textBox(Sizing.fixed(140));
    private final TextBoxComponent positionYbox = UIComponents.textBox(Sizing.fixed(140));
    private final TextBoxComponent positionZbox = UIComponents.textBox(Sizing.fixed(140));

    private final ButtonComponent zeroX = UIComponents.button(net.minecraft.network.chat.Component.literal("0"), (press -> rotXSlider.setFromDiscreteValue(0)));
    private final ButtonComponent zeroY = UIComponents.button(net.minecraft.network.chat.Component.literal("0"), (press -> rotYSlider.setFromDiscreteValue(0)));
    private final ButtonComponent zeroZ = UIComponents.button(net.minecraft.network.chat.Component.literal("0"), (press -> rotZSlider.setFromDiscreteValue(0)));

    private final DiscreteSliderComponent transXSlider = UIComponents.discreteSlider(Sizing.fixed(140), -2.0, 2.0).decimalPlaces(2);
    private final DiscreteSliderComponent transYSlider = UIComponents.discreteSlider(Sizing.fixed(140), -2.0, 2.0).decimalPlaces(2);
    private final DiscreteSliderComponent transZSlider = UIComponents.discreteSlider(Sizing.fixed(140), -2.0, 2.0).decimalPlaces(2);



    ButtonComponent tZeroX = UIComponents.button(net.minecraft.network.chat.Component.literal("0"), press -> transXSlider.setFromDiscreteValue(0.0));
    ButtonComponent tZeroY = UIComponents.button(net.minecraft.network.chat.Component.literal("0"), press -> transYSlider.setFromDiscreteValue(0.0));
    ButtonComponent tZeroZ = UIComponents.button(net.minecraft.network.chat.Component.literal("0"), press -> transZSlider.setFromDiscreteValue(0.0));

    // Navigation
    private final ButtonComponent nextEntity = UIComponents.button(net.minecraft.network.chat.Component.literal("Next"), btn -> switchEntity(1));
    private final ButtonComponent previousEntity = UIComponents.button(net.minecraft.network.chat.Component.literal("Previous"), btn -> switchEntity(-1));
    private final ButtonComponent closestEntity = UIComponents.button(net.minecraft.network.chat.Component.literal("Closest"), btn -> selectClosest());



    // Buttons
    private final ButtonComponent advancedModeButton = UIComponents.button(net.minecraft.network.chat.Component.literal("Advanced Mode"), btn -> PivotClient.advancedmode = !PivotClient.advancedmode);
    private final ButtonComponent toggleGlowButton = UIComponents.button(net.minecraft.network.chat.Component.literal("Toggle Glowing"), btn -> PivotClient.glowing = !PivotClient.glowing);

    private final ButtonComponent lockButton = UIComponents.button(net.minecraft.network.chat.Component.literal(""), press -> toggleLockDisplayEntity());

    public BlockDisplayEntityEditScreen() {
        super(net.minecraft.network.chat.Component.literal(""));
    }





    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }


    @Override
    protected void build(FlowLayout root) {
        // --- Next / Previous (top-right) ---
        FlowLayout buttons = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        buttons.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        buttons.positioning(Positioning.absolute(this.width - 125, 10));
        buttons.child(nextEntity.horizontalSizing(Sizing.fixed(110)));
        buttons.child(previousEntity.horizontalSizing(Sizing.fixed(110)));
        buttons.child(closestEntity.horizontalSizing(Sizing.fixed(110)));

        // --- Rotation Sliders ---
        FlowLayout rotationSliders = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        rotationSliders.alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP);

        Function<DiscreteSliderComponent, FlowLayout> rotationRow = (slider) -> {
            FlowLayout row = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24));
            row.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            row.child(slider.horizontalSizing(Sizing.fixed(140)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("0"), btn -> slider.setFromDiscreteValue(0)).horizontalSizing(Sizing.fixed(20)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("-45"), btn -> slider.setFromDiscreteValue(slider.discreteValue() - 45)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("+45"), btn -> slider.setFromDiscreteValue(slider.discreteValue() + 45)));
            return row;
        };

        rotXSlider.scrollStep(0.00003);
        rotYSlider.scrollStep(0.00003);
        rotZSlider.scrollStep(0.00003);

        scaleXSlider.scrollStep(0.001);
        scaleYSlider.scrollStep(0.001);
        scaleZSlider.scrollStep(0.001);

        transXSlider.scrollStep(0.003);
        transYSlider.scrollStep(0.003);
        transZSlider.scrollStep(0.003);

        rotationSliders.child(UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24))
                .child(UIComponents.label(net.minecraft.network.chat.Component.literal("Rotation X")).horizontalSizing(Sizing.fixed(70)))
                .child(rotationRow.apply(rotXSlider))
        );
        rotationSliders.child(UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24))
                .child(UIComponents.label(net.minecraft.network.chat.Component.literal("Rotation Y")).horizontalSizing(Sizing.fixed(70)))
                .child(rotationRow.apply(rotYSlider))
        );
        rotationSliders.child(UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24))
                .child(UIComponents.label(net.minecraft.network.chat.Component.literal("Rotation Z")).horizontalSizing(Sizing.fixed(70)))
                .child(rotationRow.apply(rotZSlider))
        );

        // --- Scale sliders ---
        BiFunction<String, DiscreteSliderComponent, FlowLayout> scaleRow = (label, slider) -> {
            FlowLayout row = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24));
            row.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            row.child(UIComponents.label(net.minecraft.network.chat.Component.literal(label)).horizontalSizing(Sizing.fixed(70)));
            row.child(slider.horizontalSizing(Sizing.fixed(140)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("1"), btn -> slider.setFromDiscreteValue(1.0)).horizontalSizing(Sizing.fixed(20)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("-0.5"), btn -> slider.setFromDiscreteValue(slider.discreteValue() - 0.5)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("+0.5"), btn -> slider.setFromDiscreteValue(slider.discreteValue() + 0.5)));
            return row;
        };

        rotationSliders.child(scaleRow.apply("Scale X", scaleXSlider));
        rotationSliders.child(scaleRow.apply("Scale Y", scaleYSlider));
        rotationSliders.child(scaleRow.apply("Scale Z", scaleZSlider));

        // --- Translation sliders ---
        BiFunction<String, DiscreteSliderComponent, FlowLayout> transRow = (label, slider) -> {
            FlowLayout row = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24));
            row.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            row.child(UIComponents.label(net.minecraft.network.chat.Component.literal(label)).horizontalSizing(Sizing.fixed(70)));
            row.child(slider.horizontalSizing(Sizing.fixed(140)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("0"), btn -> slider.setFromDiscreteValue(0.0)).horizontalSizing(Sizing.fixed(20)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("-0.05"), btn -> slider.setFromDiscreteValue(slider.discreteValue() - 0.05)));
            row.child(UIComponents.button(net.minecraft.network.chat.Component.literal("+0.05"), btn -> slider.setFromDiscreteValue(slider.discreteValue() + 0.05)));
            return row;
        };

        rotationSliders.child(transRow.apply("Translation X", transXSlider));
        rotationSliders.child(transRow.apply("Translation Y", transYSlider));
        rotationSliders.child(transRow.apply("Translation Z", transZSlider));

        // --- Left column wrapper ---
        FlowLayout leftColumn = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        leftColumn.alignment(HorizontalAlignment.LEFT, VerticalAlignment.TOP);
        leftColumn.positioning(Positioning.absolute(10, 50));
        leftColumn.child(rotationSliders);

        // --- Advanced Mode button ---
        FlowLayout advWrap = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        advWrap.positioning(Positioning.absolute(10, 10));
        advWrap.gap(5);
       // advWrap.child(advancedModeButton);
        advWrap.child(toggleGlowButton);



        FlowLayout newEntityWrapper = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        newEntityWrapper.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

        newEntityWrapper.child(
                UIComponents.button(net.minecraft.network.chat.Component.literal("New Block Display Entity"), press -> newBlockdisplayEntity())
                        .horizontalSizing(Sizing.fixed(130))
        );

        newEntityWrapper.child(
                UIComponents.button(net.minecraft.network.chat.Component.literal("New Item Display Entity"), press -> newItemdisplayEntity())
                        .horizontalSizing(Sizing.fixed(130))
        );

        newEntityWrapper.positioning(Positioning.absolute(this.width - 125 - 10, 230));




        newEntityWrapper.child(
                UIComponents.button(net.minecraft.network.chat.Component.literal("Remove Display Entity"), press -> removeDisplayEntity())
                        .horizontalSizing(Sizing.fixed(130))
        );

        newEntityWrapper.child(
                UIComponents.button(net.minecraft.network.chat.Component.literal("Duplicate Display Entity"), press -> duplicateDisplayEntity())
                        .horizontalSizing(Sizing.fixed(130))
        );



        newEntityWrapper.child(lockButton.horizontalSizing(Sizing.fixed(130)));

        newEntityWrapper.positioning(Positioning.absolute(this.width - 125 - 10, 225));




        FlowLayout posWrapper = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        posWrapper.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);


        FlowLayout applyRow = UIContainers.horizontalFlow(Sizing.fixed(200), Sizing.content());
        applyRow.alignment(HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);

        applyRow.child(
                UIComponents.button(net.minecraft.network.chat.Component.literal("Apply Position"), btn -> {
                    if (displayEntity != null) {
                        try {
                            double newX = Double.parseDouble(positionXbox.getValue());
                            double newY = Double.parseDouble(positionYbox.getValue());
                            double newZ = Double.parseDouble(positionZbox.getValue());
                            sendPositionUpdate(newX, newY, newZ);
                        } catch (NumberFormatException ignored) {}
                    }
                }).horizontalSizing(Sizing.fixed(129))
        );


// add above the X/Y/Z rows
        posWrapper.child(applyRow);

        // X row
        // X row
        FlowLayout xRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24));
        xRow.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        xRow.child(UIComponents.label(net.minecraft.network.chat.Component.literal("Position X")).horizontalSizing(Sizing.fixed(70)));
        positionXbox.setMaxLength(12);
        positionXbox.setFilter(s -> s.matches("-?\\d*(\\.\\d+)?"));
        xRow.child(positionXbox);
// -1 / +1 buttons
        xRow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("-1"), btn -> {
            if (displayEntity != null) {
                double newX = displayEntity.getX() - 1;
                sendPositionUpdate(newX, displayEntity.getY(), displayEntity.getZ());
                positionXbox.setValue(String.valueOf(newX));
            }
        }).horizontalSizing(Sizing.fixed(30)));
        xRow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("+1"), btn -> {
            if (displayEntity != null) {
                double newX = displayEntity.getX() + 1;
                sendPositionUpdate(newX, displayEntity.getY(), displayEntity.getZ());
                positionXbox.setValue(String.valueOf(newX));
            }
        }).horizontalSizing(Sizing.fixed(30)));
        posWrapper.child(xRow);

        // Y row
        FlowLayout yRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24));
        yRow.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        yRow.child(UIComponents.label(net.minecraft.network.chat.Component.literal("Position Y")).horizontalSizing(Sizing.fixed(70)));
        positionYbox.setMaxLength(12);
        positionYbox.setFilter(s -> s.matches("-?\\d*(\\.\\d+)?"));
        yRow.child(positionYbox);
// -1 / +1 buttons
        yRow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("-1"), btn -> {
            if (displayEntity != null) {
                double newY = displayEntity.getY() - 1;
                sendPositionUpdate( displayEntity.getX(), newY, displayEntity.getZ());
                positionYbox.setValue(String.valueOf(newY));
            }
        }).horizontalSizing(Sizing.fixed(30)));
        yRow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("+1"), btn -> {
            if (displayEntity != null) {
                double newY = displayEntity.getY() + 1;
                sendPositionUpdate( displayEntity.getX(), newY, displayEntity.getZ());
                positionYbox.setValue(String.valueOf(newY));
            }
        }).horizontalSizing(Sizing.fixed(30)));
        posWrapper.child(yRow);

        FlowLayout zRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(24));
        zRow.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        zRow.child(UIComponents.label(net.minecraft.network.chat.Component.literal("Position Z")).horizontalSizing(Sizing.fixed(70)));
        positionZbox.setMaxLength(12);
        positionZbox.setFilter(s -> s.matches("-?\\d*(\\.\\d+)?"));
        zRow.child(positionZbox);
// -1 / +1 buttons
        zRow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("-1"), btn -> {
            if (displayEntity != null) {
                double newZ = displayEntity.getZ() - 1;
                sendPositionUpdate( displayEntity.getX(), displayEntity.getY(),  newZ);
                positionZbox.setValue(String.valueOf(newZ));
            }
        }).horizontalSizing(Sizing.fixed(30)));
        zRow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("+1"), btn -> {
            if (displayEntity != null) {
                double newZ = displayEntity.getZ() + 1;
                sendPositionUpdate( displayEntity.getX(),  displayEntity.getY(),  newZ);
                positionZbox.setValue(String.valueOf(newZ));
            }
        }).horizontalSizing(Sizing.fixed(30)));
        posWrapper.child(zRow);


        /*
        // hook text box updates
        positionXbox.onChanged().subscribe(text -> {
            if (displayEntity != null && !text.isEmpty()) {
                try {
                    double val = Double.parseDouble(text);
                    sendPositionUpdate( val, displayEntity.getY(), displayEntity.getZ());
                } catch (NumberFormatException ignored) {}
            }
        });

        positionYbox.onChanged().subscribe(text -> {
            if (displayEntity != null && !text.isEmpty()) {
                try {
                    float val = Float.parseFloat(text);
                    sendPositionUpdate( displayEntity.getX(), val, displayEntity.getZ());
                } catch (NumberFormatException ignored) {}
            }
        });

        positionZbox.onChanged().subscribe(text -> {
            if (displayEntity != null && !text.isEmpty()) {
                try {
                    double val = Double.parseDouble(text);
                    sendPositionUpdate( displayEntity.getX(), displayEntity.getY(), val);
                } catch (NumberFormatException ignored) {}
            }
        });
         */

        blockStateFlow = UIContainers.verticalFlow(Sizing.content(), Sizing.content());// optional spacing




        blockStateScroll = UIContainers.verticalScroll(
                        Sizing.fixed(220), // width of scroll area
                        Sizing.fixed(130), // height of scroll area
                        blockStateFlow
                );

                blockStateScroll.scrollbarThiccness(20)
                .scrollbar(ScrollContainer.Scrollbar.vanilla())
                .padding(Insets.of(5))
                .surface(Surface.DARK_PANEL);







// Position in your UI
        blockStateScroll.positioning(Positioning.absolute(this.width - 225, 340));

// Add to root
        root.child(blockStateScroll);



        // position block on screen
        posWrapper.positioning(Positioning.absolute(10, 387));
        root.child(posWrapper);





        FlowLayout dropdownWrapper = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        dropdownWrapper.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

        dropdownWrapper.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Align X"), press -> alignCoord("x")).horizontalSizing(Sizing.fixed(80)));
        dropdownWrapper.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Align Y"), press -> alignCoord("y")).horizontalSizing(Sizing.fixed(80)));
        dropdownWrapper.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Align Z"), press -> alignCoord("z")).horizontalSizing(Sizing.fixed(80)));

        dropdownWrapper.positioning(Positioning.absolute(this.width - 125 - 10, 150));

        root.child(dropdownWrapper);



        // --- Add everything to root ---
        root.child(advWrap);
        root.child(buttons);
        root.child(leftColumn);

        root.child(newEntityWrapper);



        FlowLayout axisControls = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        axisControls.alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER);


    }

    private <T extends Comparable<T>> void handleBlockStateClick(Property<T> property, BlockState state) {
        if (displayEntity instanceof Display.BlockDisplay blockDisplayEntity) {
            T current = state.getValue(property);
            T next = getNextValue(property, current);


            ClientPlayNetworking.send(
                    new SetBlockDisplayBlockstate(
                            blockDisplayEntity.getStringUUID(),
                            property.getName(),
                            property.getName(next) // ✅ correct serialization
                    )
            );

            tick = 0;

        }
    }

    // cycles through property values
    private static <T extends Comparable<T>> T getNextValue(Property<T> property, T current) {
        ImmutableList<T> values = ImmutableList.copyOf(property.getPossibleValues());
        int idx = values.indexOf(current);
        return values.get((idx + 1) % values.size());
    }


    private void newItemdisplayEntity() {
        NewItemDisplayEntity NewItemDisplayEntity = new NewItemDisplayEntity("");
        ClientPlayNetworking.send(NewItemDisplayEntity);
    }


    private void toggleLockDisplayEntity() {
        if  (displayEntity == null) return;

        LockDisplayEntity LockDisplayEntity = new LockDisplayEntity(displayEntity.getStringUUID());

        ClientPlayNetworking.send(LockDisplayEntity);






    }

    private void duplicateDisplayEntity() {
        if  (displayEntity == null) return;

        DuplicateBlockDisplay duplicateBlockDisplay = new DuplicateBlockDisplay(displayEntity.getStringUUID());

        ClientPlayNetworking.send(duplicateBlockDisplay);







    }


    private void sendPositionUpdate(double x, double y, double z) {
        if (displayEntity == null) return;

        UpdateBlockDisplayPos updateBlockDisplayPos = new UpdateBlockDisplayPos(displayEntity.getStringUUID(), new Vec3(x, y, z));


        ClientPlayNetworking.send(updateBlockDisplayPos);
    }

    public void removeDisplayEntity() {
        if (displayEntity == null) return;

        TakebackBlockEntity TakebackBlockEntity = new TakebackBlockEntity(displayEntity.getStringUUID());
        ClientPlayNetworking.send(TakebackBlockEntity);
    }

    public void newBlockdisplayEntity() {

        NewBlockDisplayEntity newBlockDisplayEntity = new NewBlockDisplayEntity("");
        ClientPlayNetworking.send(newBlockDisplayEntity);






    }


    public void alignCoord(String coord) {
        if (displayEntity == null) return;
        positionXbox.setValue(String.valueOf(Float.valueOf((float) displayEntity.getX())));
        positionYbox.setValue(String.valueOf(Float.valueOf((float) displayEntity.getY())));
        positionZbox.setValue(String.valueOf(Float.valueOf((float) displayEntity.getZ())));


        AlignXYZ alignXYZ = new AlignXYZ(displayEntity.getStringUUID(), coord);


        Scheduler.runLater(() -> {tick = 0;}, 2);

        ClientPlayNetworking.send(alignXYZ);

    }


    @Override
    protected void init() {
        super.init();

        // Euler slider updates
        rotXSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updateBlockRotationFromEuler(); });
        rotYSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updateBlockRotationFromEuler(); });
        rotZSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updateBlockRotationFromEuler(); });

        scaleXSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updateScale(); });
        scaleYSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updateScale(); });
        scaleZSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updateScale(); });


        transXSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updatetransformation(); });
        transYSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updatetransformation(); });
        transZSlider.onChanged().subscribe(v -> { if (!suppressUpdates) updatetransformation(); });







    }


    private void updateScale() {
        if (displayEntity == null) return;

        Vector3f q = new Vector3f(
                (float) scaleXSlider.discreteValue(),
                (float) scaleYSlider.discreteValue(),
                (float) scaleZSlider.discreteValue()
        );

        sendScaleUpdate(q);
        suppressUpdates = true;

        scaleXSlider.setFromDiscreteValue(q.x);
        scaleYSlider.setFromDiscreteValue(q.y);
        scaleZSlider.setFromDiscreteValue(q.z);
        suppressUpdates = false;
    }

    private void updatetransformation() {
        if (displayEntity == null) return;

        Vector3f q = new Vector3f(
                (float) transXSlider.discreteValue(),
                (float) transYSlider.discreteValue(),
                (float) transZSlider.discreteValue()
        );

        sendTransUpdate(q);
        suppressUpdates = true;

        transXSlider.setFromDiscreteValue(q.x);
        transYSlider.setFromDiscreteValue(q.y);
        transZSlider.setFromDiscreteValue(q.z);
        suppressUpdates = false;
    }





    private void switchEntity(int direction) {
        if (PivotClient.DisplayEntitiesInWorld.isEmpty()) return;
        PivotClient.currentIndex = (PivotClient.currentIndex + direction + PivotClient.DisplayEntitiesInWorld.size()) % PivotClient.DisplayEntitiesInWorld.size();
        tick = 0;
    }

    private void updateBlockRotationFromEuler() {
        if (displayEntity == null) return;

        Quaternionf q = eulerDegreesToQuaternion(
                (float) rotXSlider.discreteValue(),
                (float) rotYSlider.discreteValue(),
                (float) rotZSlider.discreteValue()
        );

        sendRotationUpdate(q);


    }

    public void setTick() {
        tick = 0;
    }


    private void selectClosest() {
        String closestUuid = null;
        double closestDistance = Double.MAX_VALUE; // Start with a very large number
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null || Minecraft.getInstance().level == null) return;

        for (UUID uuid : PivotClient.DisplayEntitiesInWorld) {
            Display displayEntity = (Display) Minecraft.getInstance().level.getEntity(uuid);
            if (displayEntity != null) {
                double distance = displayEntity.distanceTo(player);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestUuid = displayEntity.getStringUUID();
                }
            }
        }

        String finalUuid = closestUuid;

        Scheduler.runLater(() -> {
            if (finalUuid != null) {
                UUID selectedBlockDisplayEntity = UUID.fromString(finalUuid);
                int i = PivotClient.DisplayEntitiesInWorld.indexOf(selectedBlockDisplayEntity);
                if (i != -1) {
                    PivotClient.currentIndex = i;
                }
            }
        }, 2);
    }


    private void sendRotationUpdate(Quaternionf q) {
        if (displayEntity == null) return;

        ClientPlayNetworking.send(new UpdateBlockDisplayPacketC2S(
                displayEntity.getUUID().toString(),
                null,
                q,
                null
        ));
    }

    private void sendScaleUpdate(Vector3f q) {
        if (displayEntity == null) return;
        ClientPlayNetworking.send(new UpdateBlockDisplayPacketC2S(
                displayEntity.getUUID().toString(),
                null,
                null,
                q
        ));
    }

    private void sendTransUpdate(Vector3f q) {
        if (displayEntity == null) return;
        ClientPlayNetworking.send(new UpdateBlockDisplayPacketC2S(
                displayEntity.getUUID().toString(),
                q,
                null,
                null
        ));
    }



    @Override
    public void tick() {
        super.tick();

        if (displayEntity != null) {
            boolean lockedDisplayEntity = Util.getLockedDisplayEntity(displayEntity);
            if (lockedDisplayEntity) {
                lockButton.setMessage(net.minecraft.network.chat.Component.literal("Unlock Display Entity"));
            } else {
                lockButton.setMessage(net.minecraft.network.chat.Component.literal("Lock Display Entity"));
            }
        } else {
            lockButton.setMessage(net.minecraft.network.chat.Component.literal("Lock Display Entity"));
        }

        if (!PivotClient.DisplayEntitiesInWorld.isEmpty()) {
            if (PivotClient.currentIndex >= PivotClient.DisplayEntitiesInWorld.size()) {
                PivotClient.currentIndex = 0;
            }
            displayEntity = PivotClient.getDisplayEntity(PivotClient.DisplayEntitiesInWorld.get(PivotClient.currentIndex));
        } else {
            displayEntity = null;
        }

        if (displayEntity instanceof Display.BlockDisplay blockDisplayEntity) {


            blockStateFlow.clearChildren();




            BlockState state = blockDisplayEntity.getBlockState();


            if (state.is(Blocks.PISTON)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Piston Head"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "pistonhead"))).horizontalSizing(Sizing.fixed(188)));
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Make Sticky"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "makesticky"))).horizontalSizing(Sizing.fixed(188)));
            }

            if (state.is(Blocks.PISTON_HEAD)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Piston"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "piston"))).horizontalSizing(Sizing.fixed(188)));
            }

            if (state.is(Blocks.STICKY_PISTON)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Non Sticky"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "nonsticky"))).horizontalSizing(Sizing.fixed(188)));
            }



            if (state.is(Blocks.DIRT)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Farmland"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "farmland"))).horizontalSizing(Sizing.fixed(188)));
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Grass"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "grass"))).horizontalSizing(Sizing.fixed(188)));
            }
            if (state.is(Blocks.FARMLAND)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Dirt"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "dirt"))).horizontalSizing(Sizing.fixed(188)));
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Grass"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "grass"))).horizontalSizing(Sizing.fixed(188)));

            }
            if (state.is(Blocks.GRASS_BLOCK)) {

                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Farmland"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "farmland"))).horizontalSizing(Sizing.fixed(188)));
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Dirt"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "dirt"))).horizontalSizing(Sizing.fixed(188)));
            }
            if (state.is(Blocks.BAMBOO)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Bamboo Shoot"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "bambooshoot"))).horizontalSizing(Sizing.fixed(188)));
            }
            if (state.is(Blocks.BAMBOO_SAPLING)) {
                blockStateFlow.child(UIComponents.button(net.minecraft.network.chat.Component.literal("Set Bamboo"), btn -> ClientPlayNetworking.send(new ChangeBlockDisplayState(displayEntity.getStringUUID(), "bambooshoot"))).horizontalSizing(Sizing.fixed(188)));

            }
            for (Property<?> property : state.getProperties()) {
                blockStateFlow.child(UIComponents.button(
                        net.minecraft.network.chat.Component.literal(property.getName() + " = " + state.getValue(property)),
                        btn -> handleBlockStateClick(property, blockDisplayEntity.getBlockState()) // always fresh state
                ).horizontalSizing(Sizing.fixed(188)));
            }

        }


        if (displayEntity instanceof Display.ItemDisplay itemDisplayEntity) {

            blockStateFlow.clearChildren();


            blockStateFlow.child(UIComponents.button(
                    net.minecraft.network.chat.Component.literal("Display = " + getName(currentContext)),
                    btn -> {
                        // cycle to next
                        currentContext = getNextDisplayContext(currentContext);

                        // update button label



                        // send to server (similar to your blockstate handling)
                        ClientPlayNetworking.send(
                                new SetItemDisplayContextPacket(
                                        displayEntity.getStringUUID(),
                                        currentContext.getSerializedName()
                                )
                        );

                        tick = 0;
                    }
            ).horizontalSizing(Sizing.fixed(188)));


        }

        // On first tick after switching entity, load values into sliders
        if (tick == 0 && displayEntity != null) {







            Transformation transformation = Display.createTransformation(displayEntity.getEntityData());
            Quaternionf rightRot = new Quaternionf(transformation.rightRotation());
            Vector3f scale = new Vector3f(transformation.scale());
            Vector3f translation = new Vector3f(transformation.translation());
            float[] eulers = quaternionToEulerDegrees(rightRot);

            suppressUpdates = true;


            rotXSlider.setFromDiscreteValue(eulers[0]);
            rotYSlider.setFromDiscreteValue(eulers[1]);
            rotZSlider.setFromDiscreteValue(eulers[2]);

            transXSlider.setFromDiscreteValue(translation.x);
            transYSlider.setFromDiscreteValue(translation.y);
            transZSlider.setFromDiscreteValue(translation.z);

            scaleXSlider.setFromDiscreteValue(scale.x);
            scaleYSlider.setFromDiscreteValue(scale.y);
            scaleZSlider.setFromDiscreteValue(scale.z);

            positionXbox.setValue(String.valueOf(Float.valueOf((float) displayEntity.getX())));
            positionYbox.setValue(String.valueOf(Float.valueOf((float) displayEntity.getY())));
            positionZbox.setValue(String.valueOf(Float.valueOf((float) displayEntity.getZ())));

            suppressUpdates = false;
        }
        tick++;


    }

    private String getName(ItemDisplayContext context) {
        switch (context.getSerializedName().toLowerCase()) {
            case "thirdperson_lefthand":
                return "3rd person lefthand";
            case "thirdperson_righthand":
                return "3rd person righthand";
            case "firstperson_lefthand":
                return "1st person lefthand";
            case "firstperson_righthand":
                return "1st person righthand";
            default:
                return context.getSerializedName(); // fallback
        }
    }


    private ItemDisplayContext getNextDisplayContext(ItemDisplayContext current) {
        ItemDisplayContext[] values = ItemDisplayContext.values();
        int nextIndex = (current.ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}