package com.vnator.adminshop.screen;

import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.client.gui.ChangeAccountButton;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.ClientLocalData;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.network.PacketMachineAccountChange;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SellerScreen extends AbstractContainerScreen<SellerMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/seller.png");
    private final BlockPos blockPos;

    private final String ownerUUID;
    private ChangeAccountButton changeAccountButton;
    private final List<BankAccount> usableAccounts = new ArrayList<>();
    // -1 if bankAccount is not in usableAccounts
    private int usableAccountsIndex;

    public SellerScreen(SellerMenu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
        Pair<String, Integer> bankAccount = ClientLocalData.getMachineAccount(this.blockPos);
        this.ownerUUID = ClientLocalData.getMachineOwner(this.blockPos);
        this.usableAccounts.clear();
        this.usableAccounts.addAll(ClientLocalData.getUsableAccounts());
        Optional<BankAccount> search = this.usableAccounts.stream().filter(account ->
                bankAccount.equals(Pair.of(account.getOwner(), account.getId()))).findAny();
        if (search.isEmpty()) {
            AdminShop.LOGGER.warn("Player does not have access to this seller!");
            this.usableAccountsIndex = -1;
        } else {
            BankAccount result = search.get();
            this.usableAccountsIndex = this.usableAccounts.indexOf(result);
        }
    }

    private Pair<String, Integer> getBankAccount() {
        return Pair.of(this.usableAccounts.get(this.usableAccountsIndex).getOwner(),
                this.usableAccounts.get(this.usableAccountsIndex).getId());
    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+119, y+62, (b) -> {
            Player player = Minecraft.getInstance().player;
            assert player != null;
            // Check if player is the owner
            if (!player.getStringUUID().equals(ownerUUID)) {
                player.sendMessage(new TextComponent("You are not the owner of this machine!"), player.getUUID());
                return;
            }
            // Change accounts
            changeAccounts();
            Minecraft.getInstance().player.sendMessage(new TextComponent("Changed account to "+
                    MojangAPI.getUsernameByUUID(getBankAccount().first)+":"+getBankAccount().second),
                    Minecraft.getInstance().player.getUUID());
        });
        addRenderableWidget(changeAccountButton);
    }

    private void changeAccounts() {
//        System.out.println("UsableAccounts: ");
//        usableAccounts.forEach(bankAccount -> {
//            System.out.println(bankAccount.getOwner()+":"+bankAccount.getId());
//        });
//        System.out.println("UsableAccountsIndex: "+usableAccountsIndex);
//        System.out.println("BankAccount: "+bankAccount.first+":"+bankAccount.second);
        // Check if bankAccount was in usableAccountsIndex
        if (this.usableAccountsIndex == -1) {
            AdminShop.LOGGER.error("BankAccount is not in usableAccountsIndex");
            return;
        }
        // Refresh usable accounts
        BankAccount bankAccount = usableAccounts.get(usableAccountsIndex);
        this.usableAccounts.clear();
        this.usableAccounts.addAll(ClientLocalData.getUsableAccounts());
        // Change account, either by resetting to first (personal) account or moving to next sorted account
        if (!this.usableAccounts.contains(bankAccount)) {
            this.usableAccountsIndex = 0;
        } else {
            this.usableAccountsIndex = (this.usableAccounts.indexOf(bankAccount) + 1) % this.usableAccounts.size();
        }
        // Send change package
        System.out.println("Registering account change with server...");
        Messages.sendToServer(new PacketMachineAccountChange(this.ownerUUID, getBankAccount().first,
                getBankAccount().second, this.blockPos));
    }
    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createChangeAccountButton(relX, relY);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderLabels(pPoseStack, pMouseX, pMouseY);
        drawString(pPoseStack, font, MojangAPI.getUsernameByUUID(getBankAccount().first)+":"+getBankAccount()
                        .second,7,62,0xffffff);
    }

    @Override
    public void render(PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
        renderBackground(pPoseStack);
        super.render(pPoseStack, mouseX, mouseY, delta);
        renderTooltip(pPoseStack, mouseX, mouseY);
    }
}