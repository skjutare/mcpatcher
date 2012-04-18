package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class ConnectedTextures extends Mod {
    public ConnectedTextures(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.CONNECTED_TEXTURES;
        author = "MCPatcher";
        description = "Connects adjacent blocks of the same type.";
        version = "1.0";
        defaultEnabled = false;

        configPanel = new ConfigPanel();

        classMods.add(new MinecraftMod());
        classMods.add(new RenderEngineMod());
        classMods.add(new BaseMod.TexturePackListMod(minecraftVersion));
        classMods.add(new BaseMod.TexturePackBaseMod(minecraftVersion));
        classMods.add(new BaseMod.IBlockAccessMod());
        classMods.add(new BlockMod());
        classMods.add(new TessellatorMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new WorldRendererMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CTM_UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CTM_UTILS_CLASS + "$TextureOverride"));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.SUPER_TESSELLATOR_CLASS));
    }

    private class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox glassCheckBox;
        private JCheckBox glassPaneCheckBox;
        private JCheckBox bookshelfCheckBox;
        private JCheckBox sandstoneCheckBox;
        private JCheckBox otherCheckBox;
        private JCheckBox outlineCheckBox;

        public ConfigPanel() {
            glassCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "glass", glassCheckBox.isSelected());
                }
            });

            glassPaneCheckBox.setVisible(false);
            glassPaneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", glassPaneCheckBox.isSelected());
                }
            });

            bookshelfCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", bookshelfCheckBox.isSelected());
                }
            });

            sandstoneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", sandstoneCheckBox.isSelected());
                }
            });

            otherCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "other", otherCheckBox.isSelected());
                }
            });

            outlineCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CONNECTED_TEXTURES, "outline", outlineCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            glassCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glass", true));
            glassPaneCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "glassPane", true));
            bookshelfCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "bookshelf", true));
            sandstoneCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "sandstone", true));
            otherCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "other", true));
            outlineCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "outline", false));
        }

        @Override
        public void save() {
        }
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            mapTexturePackList();

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;")));
        }
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            classSignatures.add(new ConstSignature(new MethodRef("org.lwjgl.opengl.GL11", "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "allocateAndSetupTexture", "(Ljava/awt/image/BufferedImage;)I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            final MethodRef getBlockTexture = new MethodRef(getDeobfClass(), "getBlockTexture", "(LIBlockAccess;IIII)I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        ILOAD, 5,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEINTERFACE),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        IRETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(getBlockTexture)
                .addXref(1, new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I"))
                .addXref(2, new MethodRef(getDeobfClass(), "getBlockTextureFromSideAndMetadata", "(II)I"))
            );
        }
    }

    private class TessellatorMod extends ClassMod {
        TessellatorMod() {
            final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
            final MethodRef startDrawingQuads = new MethodRef(getDeobfClass(), "startDrawingQuads", "()V");
            final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(I)V");
            final MethodRef constructor = new MethodRef("Tessellator", "<init>", "(I)V");
            final MethodRef setNormal = new MethodRef(getDeobfClass(), "setNormal", "(FFF)V");
            final MethodRef setBrightness = new MethodRef(getDeobfClass(), "setBrightness", "(I)V");
            final MethodRef setColorRGBA = new MethodRef(getDeobfClass(), "setColorRGBA", "(IIII)V");
            final MethodRef setTextureUV = new MethodRef(getDeobfClass(), "setTextureUV", "(DD)V");
            final MethodRef setTranslation = new MethodRef(getDeobfClass(), "setTranslation", "(DDD)V");
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellator;");
            final FieldRef hasNormals = new FieldRef(getDeobfClass(), "hasNormals", "Z");
            final FieldRef normal = new FieldRef(getDeobfClass(), "normal", "I");
            final FieldRef isDrawing = new FieldRef(getDeobfClass(), "isDrawing", "Z");
            final FieldRef drawMode = new FieldRef(getDeobfClass(), "drawMode", "I");
            final FieldRef hasBrightness = new FieldRef(getDeobfClass(), "hasBrightness", "Z");
            final FieldRef brightness = new FieldRef(getDeobfClass(), "brightness", "I");
            final FieldRef isColorDisabled = new FieldRef(getDeobfClass(), "isColorDisabled", "Z");
            final FieldRef hasColor = new FieldRef(getDeobfClass(), "hasColor", "Z");
            final FieldRef color = new FieldRef(getDeobfClass(), "color", "I");
            final FieldRef hasTexture = new FieldRef(getDeobfClass(), "hasTexture", "Z");
            final FieldRef textureU = new FieldRef(getDeobfClass(), "textureU", "D");
            final FieldRef textureV = new FieldRef(getDeobfClass(), "textureV", "D");
            final FieldRef xOffset = new FieldRef(getDeobfClass(), "xOffset", "D");
            final FieldRef yOffset = new FieldRef(getDeobfClass(), "yOffset", "D");
            final FieldRef zOffset = new FieldRef(getDeobfClass(), "zOffset", "D");
            final FieldRef texture = new FieldRef(getDeobfClass(), "texture", "I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("Not tesselating!")
                    );
                }
            }.setMethod(draw));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),

                        BinaryRegex.any(0, 50),

                        push("Already tesselating!"),
                        BinaryRegex.any(0, 100),

                        ALOAD_0,
                        BytecodeMatcher.captureReference(INVOKESPECIAL),

                        ALOAD_0,
                        ILOAD_1,
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(startDrawing)
                .addXref(1, isDrawing)
                .addXref(2, reset)
                .addXref(3, drawMode)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        push(7),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        RETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        push(1),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        FLOAD_1,
                        push(127.0f),
                        FMUL,
                        F2I,
                        I2B,

                        BinaryRegex.any(0, 100),

                        IOR,
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(setNormal)
                .addXref(1, hasNormals)
                .addXref(2, normal)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        ICONST_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        ALOAD_0,
                        ILOAD_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        RETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(setBrightness)
                .addXref(1, hasBrightness)
                .addXref(2, brightness)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),

                        BinaryRegex.any(0, 200),

                        ALOAD_0,
                        ICONST_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        BinaryRegex.any(0, 100),

                        reference(GETSTATIC, new FieldRef("java/nio/ByteOrder", "LITTLE_ENDIAN", "Ljava/nio/ByteOrder;")),

                        BinaryRegex.any(0, 100),

                        IOR,
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(setColorRGBA)
                .addXref(1, isColorDisabled)
                .addXref(2, hasColor)
                .addXref(3, color)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        ICONST_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD_3,
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(setTextureUV)
                .addXref(1, hasTexture)
                .addXref(2, textureU)
                .addXref(3, textureV)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD_3,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 5,
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(setTranslation)
                .addXref(1, xOffset)
                .addXref(2, yOffset)
                .addXref(3, zOffset)
            );

            memberMappers.add(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));

            for (JavaRef ref : new JavaRef[]{
                constructor, startDrawing, isDrawing, drawMode, setNormal, hasNormals, normal,
                setBrightness, hasBrightness, brightness, setColorRGBA, isColorDisabled, hasColor, color,
                hasTexture, textureU, textureV, setTranslation, xOffset, yOffset, zOffset, reset
            }) {
                patches.add(new MakeMemberPublicPatch(ref));
            }

            patches.add(new AddFieldPatch(texture));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace tessellator instance";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isStaticInitializer()) {
                        return buildExpression(
                            reference(NEW, new ClassRef("Tessellator")),
                            DUP,
                            BinaryRegex.capture(BytecodeMatcher.anyLDC),
                            reference(INVOKESPECIAL, constructor)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(NEW, new ClassRef(MCPatcherUtils.SUPER_TESSELLATOR_CLASS)),
                        DUP,
                        getCaptureGroup(1),
                        reference(INVOKESPECIAL, new MethodRef(MCPatcherUtils.SUPER_TESSELLATOR_CLASS, "<init>", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "initialize texture field to -1";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        push(-1),
                        reference(PUTFIELD, texture)
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "bind texture before drawing";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/IntBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (texture >= 0) {
                        ALOAD_0,
                        reference(GETFIELD, texture),
                        IFLT, branch("A"),

                        // GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                        push(3553), // GL11.GL_TEXTURE_2D
                        ALOAD_0,
                        reference(GETFIELD, texture),
                        reference(INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glBindTexture", "(II)V")),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(draw));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "fix references to reset method";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESPECIAL, reset)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKEVIRTUAL, reset)
                    );
                }
            });
        }

        @Override
        public void prePatch(String filename, ClassFile classFile) {
            getClassMap().addInheritance(getDeobfClass(), MCPatcherUtils.SUPER_TESSELLATOR_CLASS);
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];
        private final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "I");
        private final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        private final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;III)Z");
        private final MethodRef setup = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "setup", "(LBlock;LIBlockAccess;IIIII)Z");
        private final MethodRef reset = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "reset", "()V");

        RenderBlocksMod() {
            setupBlockFace(0, "Bottom");
            setupBlockFace(1, "Top");
            setupBlockFace(2, "North");
            setupBlockFace(3, "South");
            setupBlockFace(4, "West");
            setupBlockFace(5, "East");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // renderStandardBlock(par1BlockBrewingStand, par2, par3, par4);
                        ALOAD_0,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        POP,

                        // overrideBlockTexture = 156;
                        ALOAD_0,
                        push(156),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .addXref(1, renderStandardBlock)
                .addXref(2, overrideBlockTexture)
            );

            memberMappers.add(new FieldMapper(blockAccess));
            memberMappers.add(new MethodMapper(faceMethods));
        }

        private void setupBlockFace(final int face, final String direction) {
            faceMethods[face] = new MethodRef(getDeobfClass(), "render" + direction + "Face", "(LBlock;DDDI)V");

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "setup connected textures (" + direction.toLowerCase() + " face)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BinaryRegex.build(
                            // tessellator = Tessellator.instance;
                            reference(GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                            ASTORE, BinaryRegex.capture(BinaryRegex.any()),

                            // if (overrideBlockTexture >= 0) {
                            ALOAD_0,
                            reference(GETFIELD, overrideBlockTexture)
                        )),
                        IFLT, BinaryRegex.any(2),

                        // texture = overrideBlockTexture;
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_0,
                            reference(GETFIELD, overrideBlockTexture),
                            ISTORE, BinaryRegex.capture(BinaryRegex.any())
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // tessellator = Tessellator.instance;
                        // if (overrideBlockTexture >= 0) {
                        getCaptureGroup(1),
                        IFLT, branch("A"),

                        // texture = overrideBlockTexture;
                        getCaptureGroup(3),

                        // CTMUtils.reset();
                        reference(INVOKESTATIC, reset),
                        GOTO, branch("B"),

                        // } else if (CTMUtils.setup(block, blockAccess, (int) x, (int) y, (int) z, face, texture)) {
                        label("A"),
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        push(face),
                        ILOAD, getCaptureGroup(4),
                        reference(INVOKESTATIC, setup),
                        IFEQ, branch("B"),

                        // texture = CTMUtils.newTextureIndex;
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.CTM_UTILS_CLASS, "newTextureIndex", "I")),
                        ISTORE, getCaptureGroup(4),

                        // tessellator = CTMUtils.newTessellator;
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.CTM_UTILS_CLASS, "newTessellator", "LTessellator;")),
                        ASTORE, getCaptureGroup(2),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(faceMethods[face]));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");
            final MethodRef start = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "start", "()V");
            final MethodRef finish = new MethodRef(MCPatcherUtils.CTM_UTILS_CLASS, "finish", "()V");

            classSignatures.add(new ConstSignature(new MethodRef("org.lwjgl.opengl.GL11", "glNewList", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.000001F)
                    );
                }
            }.setMethod(updateRenderer));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "pre render standard block";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, start)
                    );
                }
            }.targetMethod(updateRenderer));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "post render standard block";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, finish),
                        RETURN
                    );
                }
            }.targetMethod(updateRenderer));
        }
    }
}