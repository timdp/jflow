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

package be.pwnt.jflow;

import java.awt.Dimension;

import be.pwnt.jflow.geometry.Point3D;
import be.pwnt.jflow.geometry.RotationMatrix;

public class Scene {
	private Point3D cameraLocation;

	private RotationMatrix cameraRotationMatrix;

	private Point3D viewerPosition;

	public Scene(Point3D cameraLocation, RotationMatrix cameraRotationMatrix,
			Point3D viewerPosition) {
		setCameraLocation(cameraLocation);
		setCameraRotationMatrix(cameraRotationMatrix);
		setViewerPosition(viewerPosition);
	}

	public Point3D getCameraLocation() {
		return cameraLocation;
	}

	public void setCameraLocation(Point3D cameraLocation) {
		this.cameraLocation = cameraLocation;
	}

	public RotationMatrix getCameraRotationMatrix() {
		return cameraRotationMatrix;
	}

	public void setCameraRotationMatrix(RotationMatrix cameraRotationMatrix) {
		this.cameraRotationMatrix = cameraRotationMatrix;
	}

	public Point3D getViewerPosition() {
		return viewerPosition;
	}

	public void setViewerPosition(Point3D viewerPosition) {
		this.viewerPosition = viewerPosition;
	}

	public Point3D project(Point3D a, Dimension surfaceSize) {
		RotationMatrix rot = getCameraRotationMatrix();
		Point3D e = getViewerPosition();
		Point3D d = new Point3D(a.subtract(getCameraLocation()));
		d.rotate(rot);
		// maintain aspect ratio
		double side = Math.max(surfaceSize.getWidth(), surfaceSize.getHeight());
		double xo = (surfaceSize.getWidth() - side) / 2;
		double yo = (surfaceSize.getHeight() - side) / 2;
		return new Point3D(xo
				+ scale((d.getX() - e.getX()) * e.getZ() / d.getZ(), side), yo
				+ scale((d.getY() - e.getY()) * e.getZ() / d.getZ(), side));
	}

	private static double scale(double coord, double max) {
		return (coord + 1) / 2 * max;
	}
}
