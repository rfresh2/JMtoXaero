
import xaero.map.misc.Misc;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.LeveledRegion;
import xaero.map.region.Overlay;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converter for Journeymap data to Xaero format
 * Based mostly on the decompiled code of xaero.map.file.MapSaveLoad
 * Written by IronException and Negative_Entropy
 */

public class JourneyMapToXaero {

    private File getFile(final IronMapRegion region) {
        return new File("C:\\Users\\mcmic\\Downloads\\Xaro.zip");
    }

    private static Random random = new Random();

    public static void main(final String[] args) {

        try {
            BufferedImage image = ImageIO.read(new File("C:\\Users\\mcmic\\Downloads\\76,55.png"));
            System.out.println(image.getRGB(0, 0));
            System.out.println(image.getRGB(511, 511));

        } catch(IOException exc) {
            System.out.println("IoException");
        }



        //new JourneyMapToXaero().saveRegion(new MapRegion("worldId", "dimId", "mwId", null, 0, 0, 0, true), 0);
        new JourneyMapToXaero().saveRegion(new IronMapRegion(), 0);

    }


    private static class IronMapRegion {


        public IronChunk getChunk(final int x, final int z) {
            return new IronChunk();
        }

        public void setChunk(final int o, final int p, final IronChunk mapTileChunk) {
            System.out.println("ok what do I do with chunk at " + o + " " + p);
            // drop it
        }

        public BranchLeveledRegion getParent() {
            return null;
        }
    }

    private static class IronChunk {

        public boolean includeInSave() {
            return true;
        }

        public boolean hasHighlightsIfUndiscovered() {
            return false; // TODO not sure about that
        }

        public LeveledRegion getLeafTexture() {
            return null;
        }

        public IronTile getTile(final int i, final int j) {
            return new IronTile();
        }

    }

    private static class IronTile {

        public boolean isLoaded() {
            return true;
        }

        public IronBlock[] getBlockColumn(final int x) {
            final IronBlock[] resul = new IronBlock[16];
            for (int i = 0; i < 16; i++) {
                resul[i] = new IronBlock(random.nextInt());
            }
            return resul;
        }

        public int getWorldInterpretationVersion() {
            return 0; // in code its that at least... :shrugging:
        }
    }

    private static class IronBlock {

        private int journeymapColor;
        private int state = 0;// TODO needs to fit with isGrass... we want that to be false I think
        private int colourType = 3; // 3 for no complexity
        private int biome = 0;

        public IronBlock(final int journeymap) {
            this.journeymapColor = journeymap;
        }


        public int getParameters() {
            final int colourTypeToWrite = this.colourType < 0 ? 0 : this.colourType & 3;
            int parameters = 1;
            parameters |= this.getNumberOfOverlays() != 0 ? 2 : 0;
            parameters |= colourTypeToWrite << 2;
            parameters |= this.biome != -1 ? 1048576 : 0;
            // ignoring grass (false), cave block (false), light (0), height (0), signed height (0)
            return parameters;
        }

        public boolean isGrass() {
            return true; // TODO not sure this is good? (this.state & -65536) == 0 && (this.state & 4095) == 2;
        }

        public int getState() {
            return this.state;
        }

        public int getTopHeight() {
            return 0; //TODO
        }

        public int getNumberOfOverlays() {
            return 0; // we prly dont need that?
        }

        public Map<Object, Object> getOverlays() {
            return Collections.emptyMap();
        }

        public int getColourType() {
            return this.colourType; // ig
        }

        public int getCustomColour() {
            return this.journeymapColor;
        }

        public int getBiome() {
            return this.biome; // thats prly plaines..
        }
    }


    public boolean saveRegion(final IronMapRegion region, final int extraAttempts) {
        try {
            final File permFile = getFile(region);
            final File file = getTempFile(permFile);
            if (!file.exists()) {
                file.createNewFile();
            }

            boolean regionIsEmpty = true;
            DataOutputStream out = null;

            try {
                final ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                out = new DataOutputStream(zipOut);
                final ZipEntry e = new ZipEntry("region.xaero");
                zipOut.putNextEntry(e);
                out.write(255);
                out.writeInt(4);
                int o = 0;

                while (true) {
                    if (o >= 8) {
                        zipOut.closeEntry();
                        break;
                    }

                    for (int p = 0; p < 8; ++p) {
                        final IronChunk chunk = region.getChunk(o, p);
                        if (chunk != null) {
                            if (!chunk.includeInSave()) {
                                if (!chunk.hasHighlightsIfUndiscovered()) {
                                    region.setChunk(o, p, (IronChunk) null);
                                    synchronized (chunk) {
                                        chunk.getLeafTexture().deleteTexturesAndBuffers();
                                    }
                                }

                                final BranchLeveledRegion parentRegion = region.getParent();
                                if (parentRegion != null) {
                                    parentRegion.setShouldCheckForUpdatesRecursive(true);
                                }
                            } else {
                                out.write(o << 4 | p);
                                boolean chunkIsEmpty = true;

                                for (int i = 0; i < 4; ++i) {
                                    for (int j = 0; j < 4; ++j) {
                                        final IronTile tile = chunk.getTile(i, j);
                                        if (tile != null && tile.isLoaded()) {
                                            chunkIsEmpty = false;

                                            for (int x = 0; x < 16; ++x) {
                                                final IronBlock[] c = tile.getBlockColumn(x);

                                                for (int z = 0; z < 16; ++z) {
                                                    this.savePixel(c[z], out);
                                                }
                                            }

                                            out.write(tile.getWorldInterpretationVersion());
                                        } else {
                                            out.writeInt(-1);
                                        }
                                    }
                                }

                                if (!chunkIsEmpty) {
                                    regionIsEmpty = false;
                                }
                            }
                        }
                    }

                    ++o;
                }
            } finally {
                if (out != null) {
                    out.close();
                }

            }

            if (regionIsEmpty) {
                this.safeDelete(permFile.toPath(), ".zip");
                this.safeDelete(file.toPath(), ".temp");
                //    if (xaero.map.WorldMap.settings.debug) {
                System.out.println("Save cancelled because the region is empty: " + region /**+ " " + region.getWorldId() + " " + region.getDimId() + " " + region.getMwId()*/);
                //   }

                return false;
            } else {
                this.safeMoveAndReplace(file.toPath(), permFile.toPath(), ".temp", ".zip");
                //      if (xaero.map.WorldMap.settings.debug) {
                System.out.println("Region saved: " + region + " " /**+ region.getWorldId() + " " + region.getDimId() + " " + region.getMwId() + ", " *+ this.mapProcessor.getMapWriter().getUpdateCounter()*/);
                // }

                return true;
            }
        } catch (final IOException var28) {
            System.out.println("IO exception while trying to save " + region
                    + " " + var28);
            if (extraAttempts > 0) {
                System.out.println("(World Map) Retrying...");

                try {
                    Thread.sleep(20L);
                } catch (final InterruptedException var25) {
                }

                return this.saveRegion(region, extraAttempts - 1);
            } else {
                return true;
            }
        }
    }

    public void safeDelete(final Path filePath, final String extension) throws IOException {
        if (!filePath.getFileName().toString().endsWith(extension)) {
            throw new RuntimeException("Incorrect file extension: " + filePath);
        } else {
            Files.deleteIfExists(filePath);
        }
    }

    public void safeMoveAndReplace(final Path fromPath, final Path toPath, final String fromExtension, final String toExtension) throws IOException {
        if (toPath.getFileName().toString().endsWith(toExtension) && fromPath.getFileName().toString().endsWith(fromExtension)) {
            Misc.safeMoveAndReplace(fromPath, toPath, true);
        } else {
            throw new RuntimeException("Incorrect file extension: " + fromPath + " " + toPath);
        }
    }


    private File getTempFile(final File permFile) {
        return new File(permFile.getParentFile(), "tempXaro.temp");
    }

    private void savePixel(final IronBlock pixel, final DataOutputStream out) throws IOException {
        final int parametres = pixel.getParameters();
        out.writeInt(parametres);
        if (!pixel.isGrass()) {
            out.writeInt(pixel.getState());
        }

        if ((parametres & 16777216) != 0) {
            System.out.println("do I actually want to save top height?");
            out.write(pixel.getTopHeight());
        }

        int biome;
        if (pixel.getNumberOfOverlays() != 0) {
            out.write(pixel.getOverlays().size());

            for (biome = 0; biome < pixel.getOverlays().size(); ++biome) {
                this.saveOverlay((Overlay) pixel.getOverlays().get(biome), out);
            }
        }

        if (pixel.getColourType() == 3) {
            out.writeInt(pixel.getCustomColour());
        }

        biome = pixel.getBiome();
        if (biome != -1) {
            if (biome < 255) {
                out.write(pixel.getBiome());
            } else {
                out.write(255);
                out.writeInt(biome);
            }
        }

    }

    private void saveOverlay(final Overlay o, final DataOutputStream out) throws IOException {
        out.writeInt(o.getParametres());
        if (!o.isWater()) {
            out.writeInt(o.getState());
        }

        if (o.getColourType() == 3) {
            out.writeInt(o.getCustomColour());
        }

        if (o.getOpacity() > 1) {
            out.writeInt(o.getOpacity());
        }

    }

}
