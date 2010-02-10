/*
 * JFlow v0.2
 * Created by Tim De Pauw <http://pwnt.be/>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.pwnt.jflow.shape;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import be.pwnt.jflow.Configuration;
import be.pwnt.jflow.Scene;
import be.pwnt.jflow.geometry.Point3D;
import be.pwnt.jflow.geometry.RotationMatrix;

public class Picture extends Rectangle {
	private BufferedImage image;

	private Point3D[] projectedPoints;

	private URL url;

	public Picture(URL url) throws IOException {
		super(new Point3D(0, 0, 0), new Point3D(0, 0, 0), Color.black);
		this.url = url;
		image = ImageIO.read(url);
		projectedPoints = new Point3D[4];
		setCoordinates(new Point3D(0, 0, 0), new Point3D(image.getWidth(),
				image.getHeight(), 0));
	}

	public int getWidth() {
		return image.getWidth();
	}

	public int getHeight() {
		return image.getHeight();
	}

	@Override
	public String toString() {
		return url.getFile();
	}

	// XXX only works for convex 2D polygons
	@Override
	public boolean contains(Point3D point) {
		if (projectedPoints[0] == null) {
			return false;
		}
		boolean side = checkSide(point, projectedPoints[0], projectedPoints[1]);
		int i = 1;
		while (i < projectedPoints.length
				&& side == checkSide(point, projectedPoints[i],
						projectedPoints[(i + 1) % projectedPoints.length])) {
			i++;
		}
		return i == projectedPoints.length;
	}

	private static boolean checkSide(Point3D point, Point3D p1, Point3D p2) {
		double c = (point.getY() - p1.getY()) * (p2.getX() - p1.getX())
				- (point.getX() - p1.getX()) * (p2.getY() - p1.getY());
		return c < 0;
	}

	// FIXME only works if same x (no horizontal distortion)
	@Override
	public void paint(Graphics graphics, Scene scene, Dimension surfaceSize,
			boolean active, Configuration config) {
		Point3D loc = getLocation();
		RotationMatrix rot = getRotationMatrix();
		List<Point3D> points = getPoints();
		Point3D[] proj = new Point3D[4];
		int i = 0;
		for (Point3D p : points) {
			Point3D pt = new Point3D(p);
			pt.rotate(rot);
			pt.translate(loc);
			proj[i] = scene.project(pt, surfaceSize);
			proj[i].setZ(pt.getZ());
			i++;
		}
		// XXX assumes too much about order
		Point3D topRight = proj[0], topLeft = proj[1], bottomLeft = proj[2], bottomRight = proj[3];
		if (topRight.getX() < 0 || topLeft.getX() >= surfaceSize.getWidth()) {
			projectedPoints[0] = null;
			return;
		}
		projectedPoints[0] = bottomLeft;
		projectedPoints[1] = bottomRight;
		projectedPoints[2] = topRight;
		projectedPoints[3] = topLeft;
		if (bottomLeft.getX() != topLeft.getX()
				|| bottomRight.getX() != topRight.getX()) {
			throw new RuntimeException();
		}
		double x0 = bottomLeft.getX();
		double x1 = bottomRight.getX();
		double y0 = Math.min(bottomLeft.getY(), bottomRight.getY());
		double y1 = Math.max(topLeft.getY(), topRight.getY());
		double heightLeft = topLeft.getY() - bottomLeft.getY();
		double heightRight = topRight.getY() - bottomRight.getY();
		int w = (int) Math.round(x1 - x0);
		int h = (int) Math.round(y1 - y0) * 2;
		if (w <= 0 || h <= 0) {
			projectedPoints[0] = null;
			return;
		}
		BufferedImage pic = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		double dt = bottomRight.getY() - bottomLeft.getY();
		boolean mirror = (dt < 0);
		if (mirror) {
			dt = -dt;
		}
		int borderRgb = config.activeShapeBorderColor.getRGB();
		for (int x = 0; x < w; x++) {
			double d = 1.0 * x / w;
			int xo = (int) Math.round(d * image.getWidth());
			double colY = dt * (mirror ? 1 - d : d);
			double colH = heightLeft + (heightRight - heightLeft) * d;
			double z = constrain(-bottomLeft.getZ()
					- (bottomRight.getZ() - bottomLeft.getZ()) * d, 0,
					Double.MAX_VALUE);
			double darkening = constrain(1 - config.darkeningFactor * z, 0, 1);
			int ys = (int) Math.round(colY);
			int ym = (int) Math.round(colY + colH);
			for (double y = ys; y < ym; y++) {
				double yr = (y - colY) / colH;
				int yo = (int) Math.round(yr * image.getHeight());
				int rgb = image.getRGB(constrain(xo, 0, image.getWidth() - 1),
						constrain(yo, 0, image.getHeight() - 1));
				rgb = setIntensity(rgb, darkening, config);
				if (config.reflectionOpacity > 0) {
					int ry = (int) Math.round(colY + colH + colH - (y - colY)
							- config.pictureReflectionOverlap);
					double alpha = yr * config.reflectionOpacity;
					int rrgb = setIntensity(rgb, alpha, config);
					pic.setRGB(x, constrain(ry, (int) Math.round(colY + colH),
							pic.getHeight() - 1), rrgb);
				}
				if (active
						&& (x < config.activeShapeBorderWidth
								|| x > w - 1 - config.activeShapeBorderWidth
								|| y < ys + config.activeShapeBorderWidth || y > ym
								- 1 - config.activeShapeBorderWidth)) {
					rgb = (config.darkenBorder ? setIntensity(borderRgb,
							darkening, config) : borderRgb);
				}
				pic.setRGB(x, constrain((int) Math.round(y), 0, (int) Math
						.round(colY + colH) - 1), rgb);
			}
		}
		graphics.drawImage(pic, (int) Math.round(x0), (int) Math.round(y0),
				null);
	}

	private static int constrain(int a, int min, int max) {
		return a < min ? min : (a > max ? max : a);
	}

	private static double constrain(double a, double min, double max) {
		return a < min ? min : (a > max ? max : a);
	}

	private static int setIntensity(int rgb, double f, Configuration config) {
		if (config.enableAlphaTransparency) {
			// XXX doesn't handle overlapping well
			return setColorOpacity(rgb, f);
		} else {
			return blendColors(rgb, config.backgroundColor.getRGB(), f);
		}
	}

	private static int blendColors(int rgb1, int rgb2, double f) {
		int rgb = 0;
		for (int i = 0; i < 32; i += 8) {
			rgb |= ((int) ((0xFF & (rgb1 >>> i)) * f + (0xFF & (rgb2 >>> i))
					* (1 - f))) << i;
		}
		return rgb;
	}

	private static int setColorOpacity(int rgb, double f) {
		int alpha = (rgb >>> 24) & 0xFF;
		alpha *= f;
		return (rgb & 0x00FFFFFF) | (alpha << 24);
	}
}
