package shiogumar.wishlist;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import shiogumar.wishlist.crops.BlockWoodPlantedMushroom;

@Mod(modid = ShiogumarsWishList.MOD_ID, name = ShiogumarsWishList.MOD_NAME, version = ShiogumarsWishList.MOD_VERSION)
@EventBusSubscriber
public class ShiogumarsWishList {
    // MOD 情報の一部。残りは mcmod.info に記載
    public static final String MOD_ID = "shiogumars_wishlist";
    public static final String MOD_NAME = "ShiogumarsWishList";
    public static final String MOD_VERSION = "0.1.1";
    public static final String LANG_PREFIX = "shiogumars_wishlist.";

    // ログ出力
    private static Logger logger;

    @ObjectHolder(MOD_ID)
    public static class BLOCKS {
        public static Block WOOD_PLANTED_BROWN_MUSHROOM = null;
        public static Block WOOD_PLANTED_RED_MUSHROOM = null;
    }

    @ObjectHolder(MOD_ID)
    public static class ITEMS {
        public static Item WOOD_PLANTED_BROWN_MUSHROOM = null;
        public static Item WOOD_PLANTED_RED_MUSHROOM = null;
    }

    private static Block setNameToBlock(Block block, String regname) {
        return setNameToBlock(block, regname, regname);
    }
    private static Block setNameToBlock(Block block, String regname, String langname) {
        return block.setRegistryName(MOD_ID, regname).setUnlocalizedName(LANG_PREFIX + langname);
    }
    private static Item setNameToItem(ItemBlock itemblock) {
        return itemblock.setRegistryName(MOD_ID, itemblock.getBlock().getRegistryName().getResourcePath());
    }

    /**
     * ブロック登録
     */
    @SubscribeEvent
    protected static void registerBlocks(RegistryEvent.Register<Block> event){
        BLOCKS.WOOD_PLANTED_BROWN_MUSHROOM = setNameToBlock((new BlockWoodPlantedMushroom())
            .setPlantedItem(Item.getItemFromBlock(Blocks.BROWN_MUSHROOM)), "wood_planted_brown_mushroom");
        BLOCKS.WOOD_PLANTED_RED_MUSHROOM = setNameToBlock((new BlockWoodPlantedMushroom())
            .setPlantedItem(Item.getItemFromBlock(Blocks.RED_MUSHROOM)), "wood_planted_red_mushroom");

        event.getRegistry().registerAll(
            BLOCKS.WOOD_PLANTED_BROWN_MUSHROOM,
            BLOCKS.WOOD_PLANTED_RED_MUSHROOM
        );
    }

    /**
     * アイテム登録
     */
    @SubscribeEvent
    protected static void registerItems(RegistryEvent.Register<Item> event) {
        ITEMS.WOOD_PLANTED_BROWN_MUSHROOM = setNameToItem(new ItemBlock(BLOCKS.WOOD_PLANTED_BROWN_MUSHROOM));
        ITEMS.WOOD_PLANTED_RED_MUSHROOM = setNameToItem(new ItemBlock(BLOCKS.WOOD_PLANTED_RED_MUSHROOM));

        event.getRegistry().registerAll(
            ITEMS.WOOD_PLANTED_BROWN_MUSHROOM,
            ITEMS.WOOD_PLANTED_RED_MUSHROOM
        );
    }

    /**
     * モデル登録（クライアント側のみ）
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    protected static void registerModels(ModelRegistryEvent event){
        ModelLoader.setCustomModelResourceLocation(ITEMS.WOOD_PLANTED_BROWN_MUSHROOM, 0, new ModelResourceLocation(ITEMS.WOOD_PLANTED_BROWN_MUSHROOM.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ITEMS.WOOD_PLANTED_RED_MUSHROOM, 0, new ModelResourceLocation(ITEMS.WOOD_PLANTED_RED_MUSHROOM.getRegistryName(), "inventory"));
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
    }
}
