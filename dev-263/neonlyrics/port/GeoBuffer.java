package dev.neonlyrics.port;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * Stand-in for the 1.21 {@code MultiBufferSource}, which no longer exists in 26.3.
 *
 * <p>Geometry is no longer written into an immediate vertex buffer: it is submitted to the level
 * renderer as a deferred draw. This class keeps the old imperative style working by buffering the
 * vertices per {@link RenderType} and replaying them into
 * {@link SubmitNodeCollector#submitCustomGeometry} when {@link #flush()} is called.
 */
public final class GeoBuffer {
    private static GeoBuffer CURRENT;

    private static final class Vtx {
        float x, y, z;
        int color = -1;
        float u, v;
        int uv1 = -1;
        int uv2 = -1;
        int overlay = -1;
        int light = -1;
        float nx, ny, nz;
        boolean normal;
    }

    private final SubmitNodeCollector collector;
    private final PoseStack pose;
    private final Map<RenderType, Sink> sinks = new LinkedHashMap<>();

    public GeoBuffer(SubmitNodeCollector collector, PoseStack pose) {
        this.collector = collector;
        this.pose = pose;
        CURRENT = this;
    }

    /** The buffer of the render pass currently being built. */
    public static GeoBuffer current() {
        return CURRENT;
    }

    /** Same call shape the 1.21 code used. */
    public VertexConsumer getBuffer(RenderType type) {
        return this.sinks.computeIfAbsent(type, k -> new Sink()).consumer;
    }

    public void text(Font font, Component text, Matrix4f matrix, float x, float y, int color, boolean shadow, int light) {
        this.text(font, text, matrix, x, y, color, shadow, light, Font.DisplayMode.NORMAL);
    }

    /**
     * World-space text. {@code Font#draw} no longer exists in 26.3; text is submitted as a
     * deferred draw in the same pose space the geometry uses.
     */
    public void text(Font font, Component text, Matrix4f matrix, float x, float y, int color,
                     boolean shadow, int light, Font.DisplayMode mode) {
        if (text == null || text.getString().isEmpty()) {
            return;
        }
        PoseStack stack = new PoseStack();
        stack.last().pose().set(matrix);
        // 26.3 slot order is (light, color, backgroundColor, outlineColor): glyphs with color 0
        // alpha=0 are fully transparent, so passing (color, 0, light, 0) rendered invisible text.
        this.collector.submitText(stack, x, y, text.getVisualOrderText(), shadow, mode, light, color, 0, 0);
    }

    /** Submits everything buffered so far to the collector. */
    public void flush() {
        for (Map.Entry<RenderType, Sink> entry : this.sinks.entrySet()) {
            Sink sink = entry.getValue();
            // The Consumer only releases a vertex when the next addVertex arrives; the last one
            // of a batch is still pending in cur here. Without this call it is dropped and the
            // following quad steals the next primitive's first corner, streaking the sky.
            sink.finish();
            if (sink.vertices.isEmpty()) {
                continue;
            }
            Replay replay = new Replay(sink.vertices);
            this.collector.submitCustomGeometry(this.pose, entry.getKey(), replay);
        }
        this.sinks.clear();
    }

    private record Replay(List<Vtx> vertices) implements SubmitNodeCollector.CustomGeometryRenderer {
        @Override
        public void render(PoseStack.Pose pose, VertexConsumer out) {
            for (Vtx vert : this.vertices) {
                out.addVertex(vert.x, vert.y, vert.z);
                out.setColor(vert.color);
                out.setUv(vert.u, vert.v);
                if (vert.uv1 >= 0) {
                    out.setUv1((vert.uv1 >> 16) & 0xFFFF, vert.uv1 & 0xFFFF);
                }
                if (vert.uv2 >= 0) {
                    out.setUv2((vert.uv2 >> 16) & 0xFFFF, vert.uv2 & 0xFFFF);
                }
                if (vert.overlay >= 0) {
                    out.setOverlay(vert.overlay);
                }
                if (vert.light >= 0) {
                    out.setLight(vert.light);
                }
                if (vert.normal) {
                    out.setNormal(vert.nx, vert.ny, vert.nz);
                }
            }
        }
    }

    private static final class Sink {
        final List<Vtx> vertices = new ArrayList<>();
        final VertexConsumer consumer = new Consumer();

        // Starts as null: the very first addVertex used to call finish() with the pre-created
        // blank Vtx still in place, pushing a stray vertex at (0,0,0) ahead of the real geometry
        // and shifting every quad out of alignment.
        private Vtx cur;

        private void finish() {
            Vtx v = this.cur;
            if (v != null) {
                this.vertices.add(v);
                this.cur = null;
            }
        }

        private final class Consumer implements VertexConsumer {
            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                Sink.this.finish();
                Vtx v = new Vtx();
                v.x = x;
                v.y = y;
                v.z = z;
                Sink.this.cur = v;
                return this;
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                Sink.this.cur.color = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
                return this;
            }

            @Override
            public VertexConsumer setColor(int argb) {
                Sink.this.cur.color = argb;
                return this;
            }

            @Override
            public VertexConsumer setUv(float u, float v) {
                Sink.this.cur.u = u;
                Sink.this.cur.v = v;
                return this;
            }

            @Override
            public VertexConsumer setUv1(int u, int v) {
                Sink.this.cur.uv1 = (u & 0xFFFF) << 16 | (v & 0xFFFF);
                return this;
            }

            @Override
            public VertexConsumer setUv2(int u, int v) {
                Sink.this.cur.uv2 = (u & 0xFFFF) << 16 | (v & 0xFFFF);
                return this;
            }

            @Override
            public VertexConsumer setUv3(float u, float v) {
                return this;
            }

            @Override
            public VertexConsumer setNormal(float x, float y, float z) {
                Sink.this.cur.nx = x;
                Sink.this.cur.ny = y;
                Sink.this.cur.nz = z;
                Sink.this.cur.normal = true;
                return this;
            }

            @Override
            public VertexConsumer setLineWidth(float width) {
                return this;
            }

            @Override
            public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
                org.joml.Vector3f out = matrix.transformPosition(x, y, z, new org.joml.Vector3f());
                return this.addVertex(out.x, out.y, out.z);
            }

            @Override
            public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
                org.joml.Vector3f out = pose.pose().transformPosition(x, y, z, new org.joml.Vector3f());
                return this.addVertex(out.x, out.y, out.z);
            }
        }
    }
}
