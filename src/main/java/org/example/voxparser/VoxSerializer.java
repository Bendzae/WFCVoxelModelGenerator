package org.example.voxparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class VoxSerializer {

  public static final byte[] VOX = new byte[]{
      (byte) 'V', (byte) 'O', (byte) 'X', (byte) ' '
  };

  public static final byte[] MAIN = new byte[]{
      (byte) 'M', (byte) 'A', (byte) 'I', (byte) 'N'
  };

  public static final byte[] SIZE = new byte[]{
      (byte) 'S', (byte) 'I', (byte) 'Z', (byte) 'E'
  };

  public static final byte[] XYZI = new byte[]{
      (byte) 'X', (byte) 'Y', (byte) 'Z', (byte) 'I'
  };

  public static final byte[] RGBA = new byte[]{
      (byte) 'R', (byte) 'G', (byte) 'B', (byte) 'A'
  };


  public VoxSerializer() {

  }

  public void writeToVox(VoxModel model, int[] palette, String filename) {

    int size_content_length = 3 * 4; // X,Y,Z * 4byte int
    int xyzi_content_length = model.getVoxels().length * 4 + 4; //4byte int N, N*4bye for voxels
    int rgba_content_length = palette.length * 4;
    int chunk_data_length = 3 * 4; // id + content size + children size
    int numberOfChunks = 3;

    File file = new File(filename);
    try (FileOutputStream fos = new FileOutputStream(file)) {
      //Write magic bytes "VOX "
      fos.write(VOX);
      //File Version
      StreamUtils.writeIntLE(fos, 150);

      //Main Chunk
      fos.write(MAIN);
      //Content bytes
      StreamUtils.writeIntLE(fos, 0);
      //Children bytes
      StreamUtils.writeIntLE(
          fos,
          size_content_length + xyzi_content_length + rgba_content_length + numberOfChunks * chunk_data_length
      );

      //SIZE Chunk
      fos.write(SIZE);
      //Content bytes
      StreamUtils.writeIntLE(fos, size_content_length);
      //Children bytes
      StreamUtils.writeIntLE(fos, 0);
      //Content
      StreamUtils.writeIntLE(fos, model.getSize().getX());
      StreamUtils.writeIntLE(fos, model.getSize().getY());
      StreamUtils.writeIntLE(fos, model.getSize().getZ());

      //XYZI Chunk
      fos.write(XYZI);
      //Content bytes
      StreamUtils.writeIntLE(fos, xyzi_content_length);
      //Children bytes
      StreamUtils.writeIntLE(fos, 0);
      //Content
      StreamUtils.writeIntLE(fos, model.getVoxels().length);
      for (Voxel voxel : model.getVoxels()) {
        byte[] voxBytes = new byte[]{
            voxel.getPosition().getX(),
            voxel.getPosition().getY(),
            voxel.getPosition().getZ(),
            (byte) voxel.getColourIndex()
        };
        fos.write(voxBytes);
      }

      //RGBA Chunk
      fos.write(RGBA);
      //Content bytes
      StreamUtils.writeIntLE(fos, rgba_content_length);
      //Children bytes
      StreamUtils.writeIntLE(fos, 0);
      //Content
      for (int i = 0; i < palette.length - 1; i++) {
        try {
          StreamUtils.writeIntLE(fos, palette[i + 1]);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      StreamUtils.writeIntLE(fos, 0);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
