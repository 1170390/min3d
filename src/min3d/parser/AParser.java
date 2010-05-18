package min3d.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import min3d.core.Object3dContainer;
import min3d.vos.Number3d;
import min3d.vos.Uv;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
 * Abstract parser class with basic parsing functionality.
 * 
 * @author dennis.ippel
 *
 */
public abstract class AParser implements IParser {
	protected Resources resources;
	protected String resourceID;
	protected String packageID;
	protected String currentMaterialKey;
	protected ArrayList<ParseObjectData> parseObjects;
	protected ParseObjectData co;
	protected boolean firstObject;
	protected TextureAtlas textureAtlas;
	protected ArrayList<Number3d> vertices;
	protected ArrayList<Uv> texCoords;
	protected ArrayList<Number3d> normals;
	
	public AParser()
	{
		vertices = new ArrayList<Number3d>();
		texCoords = new ArrayList<Uv>();
		normals = new ArrayList<Number3d>();
		parseObjects = new ArrayList<ParseObjectData>();
		textureAtlas = new TextureAtlas();
		firstObject = true;
	}
	
	/**
	 * Override this in the concrete parser
	 */
	@Override
	public Object3dContainer getParsedObject() {
		return null;
	}

	/**
	 * Override this in the concrete parser
	 */
	@Override
	public void parse() {
	}
	
	/**
	 * Contains texture information. UV offsets and scaling is stored here.
	 * This is used with texture atlases.
	 * 
	 * @author dennis.ippel
	 *
	 */
	protected class BitmapAsset
	{
		/**
		 * The texture bitmap
		 */
		public Bitmap bitmap;
		/**
		 * The texture identifier
		 */
		public String key;
		/**
		 * U-coordinate offset
		 */
		public float uOffset;
		/**
		 * V-coordinate offset
		 */
		public float vOffset;
		/**
		 * U-coordinate scaling value
		 */
		public float uScale;
		/**
		 * V-coordinate scaling value
		 */
		public float vScale;
		
		/**
		 * Creates a new BitmapAsset object
		 * @param bitmap
		 * @param key
		 */
		public BitmapAsset(Bitmap bitmap, String key)
		{
			this.bitmap = bitmap;
			this.key = key;
		}
	}
	
	/**
	 * When a model contains per-face textures a texture atlas is created. This
	 * combines multiple textures into one and re-calculates the UV coordinates.
	 * 
	 * @author dennis.ippel
	 * 
	 */
	protected class TextureAtlas {
		/**
		 * The texture bitmaps that should be combined into one.
		 */
		private ArrayList<BitmapAsset> bitmaps;
		/**
		 * The texture atlas bitmap
		 */
		private Bitmap atlas;

		/**
		 * Creates a new texture atlas instance.
		 */
		public TextureAtlas() {
			bitmaps = new ArrayList<BitmapAsset>();
		}

		/**
		 * Adds a bitmap to the atlas
		 * 
		 * @param bitmap
		 */
		public void addBitmapAsset(BitmapAsset bitmap) {
			bitmaps.add(bitmap);
		}

		/**
		 * Generates a new texture atlas
		 */
		public void generate() {
			Collections.sort(bitmaps, new BitmapHeightComparer());

			if(bitmaps.size() == 0) return;
			
			BitmapAsset largestBitmap = bitmaps.get(0);
			int totalWidth = 0;
			int numBitmaps = bitmaps.size();

			for (int i = 0; i < numBitmaps; i++) {
				totalWidth += bitmaps.get(i).bitmap.getWidth();
			}

			atlas = Bitmap.createBitmap(totalWidth, largestBitmap.bitmap
					.getHeight(), Config.ARGB_8888);
			int uOffset = 0;
			int vOffset = 0;

			for (int i = 0; i < numBitmaps; i++) {
				BitmapAsset ba = bitmaps.get(i);
				Bitmap b = ba.bitmap;
				int w = b.getWidth();
				int h = b.getHeight();
				int[] pixels = new int[w * h];

				b.getPixels(pixels, 0, w, 0, 0, w, h);
				atlas.setPixels(pixels, 0, w, uOffset, vOffset, w, h);

				ba.uOffset = (float) uOffset / totalWidth;
				ba.vOffset = 0;
				ba.uScale = (float) w / (float) totalWidth;
				ba.vScale = (float) h
						/ (float) largestBitmap.bitmap.getHeight();

				uOffset += w;

				b.recycle();
			}
		}

		/**
		 * Returns the generated texture atlas bitmap
		 * 
		 * @return
		 */
		public Bitmap getBitmap() {
			return atlas;
		}

		/**
		 * Indicates whether bitmaps have been added to the atlas.
		 * 
		 * @return
		 */
		public boolean hasBitmaps() {
			return bitmaps.size() > 0;
		}

		/**
		 * Compares the height of two BitmapAsset objects.
		 * 
		 * @author dennis.ippel
		 * 
		 */
		private class BitmapHeightComparer implements Comparator<BitmapAsset> {
			public int compare(BitmapAsset b1, BitmapAsset b2) {
				int height1 = b1.bitmap.getHeight();
				int height2 = b2.bitmap.getHeight();

				if (height1 < height2) {
					return 1;
				} else if (height1 == height2) {
					return 0;
				} else {
					return -1;
				}
			}
		}

		/**
		 * Returns a bitmap asset with a specified name.
		 * 
		 * @param materialKey
		 * @return
		 */
		public BitmapAsset getBitmapAssetByName(String materialKey) {
			int numBitmaps = bitmaps.size();

			for (int i = 0; i < numBitmaps; i++) {
				if (bitmaps.get(i).key.equals(materialKey))
					return bitmaps.get(i);
			}

			return null;
		}
		
		public void cleanup()
		{
			int numBitmaps = bitmaps.size();

			for (int i = 0; i < numBitmaps; i++) {
				bitmaps.get(i).bitmap.recycle();
			}
			
			atlas.recycle();
			bitmaps.clear();
			vertices.clear();
			texCoords.clear();
			normals.clear();
		}
	}
}
