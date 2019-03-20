package shiogumar.wishlist.crops;

import javafx.geometry.Pos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

// ブロック「キノコを植えた原木」
public class BlockWoodPlantedMushroom extends BlockHorizontal implements IGrowable
{
    // 向きのメタ値に2ビット使うので、残り2ビットが成長度合い。MAX_AGE は最高で 3 まで可能。
    private static final int MAX_AGE = 2;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MAX_AGE);
    protected static final AxisAlignedBB BUSH_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected Item plantedItem = Item.getItemFromBlock(Blocks.BROWN_MUSHROOM);

    public BlockWoodPlantedMushroom()
    {
        this(Material.PLANTS);
    }

    public BlockWoodPlantedMushroom(Material materialIn)
  {
      this(materialIn, materialIn.getMaterialMapColor());
  }

    public BlockWoodPlantedMushroom(Material materialIn, MapColor mapColorIn)
    {
        super(materialIn, mapColorIn);
        this.setDefaultState(this.blockState.getBaseState().withProperty(this.getAgeProperty(), Integer.valueOf(0)).withProperty(this.getFacingProperty(), EnumFacing.NORTH));
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setHardness(2.0F).setResistance(5.0F).setHarvestLevel("axe", 0);
        this.setSoundType(SoundType.PLANT);
        this.setPlantedItem(Item.getItemFromBlock(Blocks.BROWN_MUSHROOM));
        this.disableStats();
    }

    //===========================================================
    // プロパティが増えたのでブロック情報を作る処理を上書き
    //===========================================================

    /** ブロック情報の作成。
     * Block のコンストラクタで実行され、この戻り値が this.blockstate の中身となる。
     * @return {BlockStateContainer}
     */
    @Override
    protected BlockStateContainer createBlockState()
    {
        return (new BlockStateContainer.Builder(this)).add(FACING, AGE).build();
    }

    //===========================================================
    // ブロックの状態はメタ値の形で保存する必要があるため、
    // プロパティとメタ値を変換する処理を上書きする
    //===========================================================

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        int age = meta & 0x3;
        EnumFacing facing = EnumFacing.getHorizontal((meta & 0xC) >> 2);
        return this.withAgeAndFacing(this.getDefaultState(), age, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        int age = this.getAge(state);
        EnumFacing facing = this.getFacing(state);
        return age | (facing.getHorizontalIndex() << 2);
    }

    //===========================================================
    // 設置方向によってブロックの向きを変えたいので、
    // 設置時の状態を決める処理を上書きする
    //===========================================================

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        IBlockState iblockstate = super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
        EnumFacing placerFacing = placer.getHorizontalFacing();
        EnumFacing blockFacing = placerFacing.getOpposite();
        return this.withAgeAndFacing(iblockstate, 0, blockFacing);
    }

    //===========================================================
    // 平らな場所の上にしか置けないように、
    // 設置可否を判定するために呼ばれる処理を上書きする。
    // 周辺のブロックが変化したときに呼ばれる処理も、
    // 設置できない状況になったら壊れるように上書きする。
    //===========================================================

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        IBlockState baseBlockState = worldIn.getBlockState(pos.down());
        return super.canPlaceBlockAt(worldIn, pos) && baseBlockState.isSideSolid(worldIn, pos.down(), EnumFacing.UP);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        // ブロックがアイテム化するかどうかを判定し、処理する関数。
        this.checkAndDropBlock(worldIn, pos, state);
    }

    //===========================================================
    // ランダムでたまに呼ばれる関数を上書きして、
    // 成長管理などを行う
    //===========================================================

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        // ブロックがアイテム化するかどうかを判定し、処理する関数。
        // ブロックが壊れたらこの先の処理を行わない。
        if (this.checkAndDropBlock(worldIn, pos, state)) return;

        // 成長に関する処理。ランダムで呼ばれた上でさらにランダム処理を行う。
        if (rand.nextInt(3) != 0)
        {
            // 周辺の光の計算が終わる前に呼ばれることがあるらしいので、
            // エリア読み込みが完了していなければこの先の処理を行わないように調整。
            if (!worldIn.isAreaLoaded(pos, 1)) return;

            // キノコは光源レベル 0 ~ 12 でしか成長しない。
            // BlockCrops ではひとつ上の座標の光源を getLightFromNeighbors で計算しているけど、
            // とりあえずこのブロック自体の光源レベルを計算する
            if (worldIn.getLight(pos) < 13)
            {
                int i = this.getAge(state);

                if (i < this.getMaxAge())
                {
                    // BlockCrops からの流用。成長の前後にフックを仕掛けることができるみたい。
                    // フックが仕掛けられた場合にはフック処理が正しく発動し、
                    // 場合によってはこの成長をキャンセルさせられるように書いておく。
                    if(net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos, state, true))
                    {
                        worldIn.setBlockState(pos, this.withAge(state, i + 1), 2);
                        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state, worldIn.getBlockState(pos));
                    }
                }
            }
        }
    }

    /**
     *
     * @param worldIn
     * @param pos
     * @param state
     * @return
     */
    protected boolean checkAndDropBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        // チャンク情報が読み込み済みでない場合は何もしない
        if(!worldIn.isAreaLoaded(pos, 1)) return false;

        // 地面が平ではない場合、このブロックはアイテム化する
        IBlockState baseBlockState = worldIn.getBlockState(pos.down());
        if (!baseBlockState.isSideSolid(worldIn, pos.down(), EnumFacing.UP)) {
            // ブロックを壊してアイテム化する Block.dropBlockAsItem 関数。
            this.dropBlockAsItem(worldIn, pos, state, 0);
            // なくなった場所にはしっかり空気ブロックをセット。
            worldIn.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            return true;
        }

        // 光源レベル 13 以上の場合、すでに成長したキノコは飛び出し、成長度合いをリセットする。
        if (worldIn.getLight(pos) >= 13) {
            if (this.getAge(state) == this.getMaxAge()) {
                ItemStack stack = this.getPlantedItem().getDefaultInstance();
                stack.setCount(2);
                this.dropToFront(worldIn, pos, state, stack);
            }
            worldIn.setBlockState(pos, this.withAge(state, 0), 2);
        }

        return false;
    }

    // Block.spawnAsEntity() を参考に作ったキノコ落とし関数
    public static void dropToFront(World worldIn, BlockPos pos, IBlockState state, ItemStack stack)
    {
        if (!worldIn.isRemote && !stack.isEmpty() && worldIn.getGameRules().getBoolean("doTileDrops")&& !worldIn.restoringBlockSnapshots) // do not drop items while restoring blockstates, prevents item dupe
        {
            if (captureDrops.get())
            {
                capturedDrops.get().add(stack);
                return;
            }

            EnumFacing front = state.getValue(FACING);
            Vec3i frontVeci = front.getDirectionVec();
            Vec3d frontVecd = new Vec3d(frontVeci.getX(), frontVeci.getY(), frontVeci.getZ());

            Vec3d centerPos = (new Vec3d(pos.getX(), pos.getY(), pos.getZ())).addVector(0.5D, 0.5D, 0.5D);
            Vec3d frontPos = centerPos.add(frontVecd.scale(0.55D));

            EntityItem entityitem = new EntityItem(worldIn, frontPos.x, frontPos.y, frontPos.z, stack);
            entityitem.setDefaultPickupDelay();
            entityitem.motionX = frontVecd.x * 0.1D;
            entityitem.motionY = frontVecd.y * 0.1D + 0.1D;
            entityitem.motionZ = frontVecd.z * 0.1D;

            worldIn.spawnEntity(entityitem);
        }
    }

    // Block.spawnAsEntity() を参考に作ったキノコ落とし関数
    public static void dropToPlayer(World worldIn, BlockPos pos, IBlockState state, ItemStack stack, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!worldIn.isRemote && !stack.isEmpty() && worldIn.getGameRules().getBoolean("doTileDrops")&& !worldIn.restoringBlockSnapshots) // do not drop items while restoring blockstates, prevents item dupe
        {
            if (captureDrops.get())
            {
                capturedDrops.get().add(stack);
                return;
            }

            Vec3d playerPos = new Vec3d(playerIn.posX, playerIn.posY, playerIn.posZ);
            EntityItem entityitem = new EntityItem(worldIn, playerPos.x, playerPos.y, playerPos.z, stack);
            entityitem.setNoPickupDelay();

            worldIn.spawnEntity(entityitem);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (this.getAge(state) == this.getMaxAge()) {
            ItemStack stack = this.getPlantedItem().getDefaultInstance();
            stack.setCount(2);
            this.dropToPlayer(worldIn, pos, state, stack, playerIn, hand, facing, hitX, hitY, hitZ);

            worldIn.setBlockState(pos, this.withAge(state, 0), 2);
            worldIn.playSound(null, pos, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    //===========================================================
    // カーソルを当てるための当たり判定と、
    // エンティティが衝突するための当たり判定。
    //===========================================================

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return BUSH_AABB;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return BUSH_AABB;
    }

    //===========================================================
    // ランダムでたまに呼ばれる関数を上書きして、
    // 成長管理などを行う
    //===========================================================

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }

  /**
   * Get the geometry of the queried face at the given position and state. This is used to decide whether things like
   * buttons are allowed to be placed on the face, or how glass panes connect to the face, among other things.
   * <p>
   * Common values are {@code SOLID}, which is the default, and {@code UNDEFINED}, which represents something that
   * does not fit the other descriptions and will generally cause other things not to connect to the face.
   *
   * @return an approximation of the form of the given face
   */
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
  {
      return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
      return !this.isMaxAge(state);
  }

  @Override
  public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
      return true;
  }

  @Override
  public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
      int i = this.getAge(state) + this.getBonemealAgeIncrease(worldIn);
      int j = this.getMaxAge();

      if (i > j)
      {
          i = j;
      }

      worldIn.setBlockState(pos, this.withAge(state, i), 2);
  }

  protected int getBonemealAgeIncrease(World worldIn)
  {
      return MathHelper.getInt(worldIn.rand, 0, 1);
  }

  protected PropertyInteger getAgeProperty()
  {
      return AGE;
  }

  public int getMaxAge()
  {
      return MAX_AGE;
  }

  protected int getAge(IBlockState state)
  {
      return state.getValue(this.getAgeProperty()).intValue();
  }

  public boolean isMaxAge(IBlockState state)
  {
      return state.getValue(this.getAgeProperty()).intValue() >= this.getMaxAge();
  }

  protected PropertyDirection getFacingProperty()
  {
    return FACING;
  }

  protected EnumFacing getFacing(IBlockState state)
  {
    return state.getValue(this.getFacingProperty());
  }

  public IBlockState withAge(IBlockState state, int age)
  {
    return state.withProperty(this.getAgeProperty(), Integer.valueOf(age));
  }

  public IBlockState withFacing(IBlockState state, EnumFacing facing)
  {
    return state.withProperty(this.getFacingProperty(), facing);
  }

    public IBlockState withAgeAndFacing(IBlockState state, int age, EnumFacing facing)
    {
        return state.withProperty(this.getAgeProperty(), Integer.valueOf(age)).withProperty(this.getFacingProperty(), facing);
    }

  /**
   * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
   * blockstate.
   */
  @Override
  public IBlockState withRotation(IBlockState state, Rotation rot)
  {
    return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
  }

  /**
   * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
   * blockstate.
   */
  @Override
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
  {
    return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
  }

  public BlockWoodPlantedMushroom setPlantedItem(Item plantedItem) {
      this.plantedItem = plantedItem;
      return this;
  }

    public Item getPlantedItem() {
        return this.plantedItem;
    }
}