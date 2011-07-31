/*
 * JFlow
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
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

	public Picture(BufferedImage image) {
		super(new Point3D(0, 0, 0), new Point3D(0, 0, 0), Color.black);
		this.image = image;
		projectedPoints = new Point3D[4];
		setCoordinates(new Point3D(0, 0, 0), new Point3D(image.getWidth(),
				image.getHeight(), 0));
	}

	public Picture(URL url) throws IOException {
		this(ImageIO.read(url));
	}

	public int getWidth() {
		return image.getWidth();
	}

	public int getHeight() {
		return image.getHeight();
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
		Graphics2D g = (Graphics2D) graphics;
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
		Point3D bottomR = proj[0], bottomL = proj[1], topL = proj[2], topR = proj[3];
		if (bottomR.getX() < 0 || bottomL.getX() >= surfaceSize.getWidth()) {
			projectedPoints[0] = null;
			return;
		}
		projectedPoints[0] = topL;
		projectedPoints[1] = topR;
		projectedPoints[2] = bottomR;
		projectedPoints[3] = bottomL;
		if (topL.getX() != bottomL.getX() || topR.getX() != bottomR.getX()) {
			throw new RuntimeException();
		}
		double x0 = topL.getX();
		double x1 = topR.getX();
		double y0 = Math.min(topL.getY(), topR.getY());
		double heightLeft = bottomL.getY() - topL.getY();
		double heightRight = bottomR.getY() - topR.getY();
		int w = (int) Math.round(x1 - x0);
		if (w <= 0) {
			projectedPoints[0] = null;
			return;
		}
		double dt = topR.getY() - topL.getY();
		boolean mirror = (dt < 0);
		if (mirror) {
			dt = -dt;
		}
		// reflection
		if (config.reflectionOpacity > 0) {
			for (int x = 0; x < w; x++) {
				double d = 1.0 * x / w;
				int xo = (int) Math.round(d * image.getWidth());
				int xt = (int) Math.round(x0 + x);
				double colY = dt * (mirror ? 1 - d : d);
				double colH = heightLeft + (heightRight - heightLeft) * d;
				int ryt0 = (int) Math.floor(y0 + colY + colH);
				int ryt1 = (int) Math.floor(y0 + colY + colH + colH);
				g.drawImage(image, xt, ryt1, xt + 1, ryt0, xo, 0, xo + 1,
						image.getHeight(), null);
			}
			int l = (int) Math.round(x0);
			int r = (int) Math.round(x0 + w);
			int[] xPoints = new int[] { l, r, r, l };
			int[] yPoints = new int[] {
					(int) Math.floor(topL.getY() + heightLeft),
					(int) Math.floor(topR.getY() + heightRight),
					(int) Math.ceil(bottomR.getY() + heightRight),
					(int) Math.ceil(bottomL.getY() + heightLeft) };
			g.setColor(getOverlayColor(1 - config.reflectionOpacity, config));
			g.fillPolygon(xPoints, yPoints, 4);
		}
		// shade & image
		for (int x = 0; x < w; x++) {
			double d = 1.0 * x / w;
			int xo = (int) Math.round(d * image.getWidth());
			int xt = (int) Math.round(x0 + x);
			double colY = dt * (mirror ? 1 - d : d);
			double colH = heightLeft + (heightRight - heightLeft) * d;
			double z = constrain(
					-topL.getZ() - (topR.getZ() - topL.getZ()) * d, 0,
					Double.MAX_VALUE);
			double ys = colY;
			double ym = colY + colH;
			int yt = (int) Math.round(y0 + ys);
			int yb = (int) Math.round(y0 + ym);
			g.drawImage(image, xt, yt, xt + 1, yb, xo, 0, xo + 1,
					image.getHeight(), null);
			float shadeOpacity = (float) constrain(config.shadingFactor * z, 0,
					1);
			if (shadeOpacity > 0) {
				g.setColor(getOverlayColor(shadeOpacity, config));
				g.drawLine(xt, yt, xt, yb + yb);
			}
		}
		// border
		if (active) {
			int l = (int) Math.round(x0);
			int r = (int) Math.round(x0 + w);
			int[] xPoints = new int[] { l, r, r, l };
			int[] yPoints = new int[] { (int) Math.round(topL.getY()),
					(int) Math.round(topR.getY()),
					(int) Math.round(bottomR.getY()),
					(int) Math.round(bottomL.getY()) };
			g.setColor(config.activeShapeBorderColor);
			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(config.activeShapeBorderWidth));
			g.drawPolygon(xPoints, yPoints, 4);
			g.setStroke(oldStroke);
		}
	}

	private static Color getOverlayColor(double opacity, Configuration config) {
		Color base = config.backgroundColor;
		return new Color(base.getRed(), base.getGreen(), base.getBlue(),
				(int) Math.round(base.getAlpha() * opacity));
	}

	private static double constrain(double a, double min, double max) {
		return a < min ? min : (a > max ? max : a);
	}
}
