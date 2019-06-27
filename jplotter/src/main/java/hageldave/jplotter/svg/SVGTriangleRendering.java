package hageldave.jplotter.svg;

import java.awt.geom.Rectangle2D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.util.Utils;

public class SVGTriangleRendering {

	public static void addSVGTriangle(
			Document doc, 
			Element trianglesGroup, 
			double[] coords, 
			int[] colors, 
			float alphaMultiplier, 
			String strategy,
			Rectangle2D viewportRect)
	{
		int c0=colors[0], c1=colors[1], c2=colors[2];
		double x0,y0, x1,y1, x2,y2;
		{
			int i=0;
			x0=coords[i++]; y0=coords[i++]; x1=coords[i++]; y1=coords[i++]; x2=coords[i++]; y2=coords[i++];
			if( !(
					viewportRect.intersectsLine(x0, y0, x1, y1) || 
					viewportRect.intersectsLine(x0, y0, x2, y2) ||
					viewportRect.intersectsLine(x2, y2, x1, y1) ||
					triangleMayContain(x0, y0, x1, y1, x2, y2, viewportRect)
				)
			){
				return;
			}
		}
		
		if(c0==c1 && c1==c2){
			strategy = "";
		} else if(maxARGBChannelDiff(c0, c1) < 4 && maxARGBChannelDiff(c1, c2) < 4){
			strategy = "AVG_COLOR";
		}
		
		switch (strategy) {
		case "SUBDIVIDE":{
			// test if subdivision is needed
			int edge = getSubdivisionEdge(x0, y0, x1, y1, x2, y2);
			if(edge==0){
				coords = new double[6];
				int c = Utils.interpolateColor(c0, c1, 0.5);
				// first subdivided triangle
				int i=0;
				coords[i++]=x0;coords[i++]=y0; coords[i++]=x0+(x1-x0)/2;coords[i++]=y0+(y1-y0)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c0; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
				// second subdivided triangle
				i=0;
				coords[i++]=x1;coords[i++]=y1; coords[i++]=x0+(x1-x0)/2;coords[i++]=y0+(y1-y0)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c1; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
			} else if(edge==1){
				coords = new double[6];
				int c = Utils.interpolateColor(c1, c2, 0.5);
				// first subdivided triangle
				int i=0;
				coords[i++]=x0;coords[i++]=y0; coords[i++]=x1+(x2-x1)/2;coords[i++]=y1+(y2-y1)/2; coords[i++]=x1;coords[i++]=y1;
				colors[0]=c0; colors[1]=c; colors[2]=c1;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
				// second subdivided triangle
				i=0;
				coords[i++]=x0;coords[i++]=y0; coords[i++]=x1+(x2-x1)/2;coords[i++]=y1+(y2-y1)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c0; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
			} else if(edge==2){
				coords = new double[6];
				int c = Utils.interpolateColor(c0, c2, 0.5);
				// first subdivided triangle
				int i=0;
				coords[i++]=x1;coords[i++]=y1; coords[i++]=x0+(x2-x0)/2;coords[i++]=y0+(y2-y0)/2; coords[i++]=x0;coords[i++]=y0;
				colors[0]=c1; colors[1]=c; colors[2]=c0;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
				// second subdivided triangle
				i=0;
				coords[i++]=x1;coords[i++]=y1; coords[i++]=x0+(x2-x0)/2;coords[i++]=y0+(y2-y0)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c1; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
			} else {
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "AVG_COLOR", viewportRect);
			}
		}
		break;
		case "AVG_COLOR":{
			int c = Utils.averageColor(c0,c1,c2);
			addSVGTriangle(doc, trianglesGroup, coords, new int[]{c,c,c}, alphaMultiplier, "", viewportRect);
		}
		break;
		default:
			Element triangle = SVGUtils.createSVGElement(doc, "path");
			trianglesGroup.appendChild(triangle);
			triangle.setAttributeNS(null, "d", "M "+SVGUtils.svgPoints(x0,y0) +" L "+SVGUtils.svgPoints(x1,y1,x2,y2)+" Z");
			triangle.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(c0));
			if(alphaMultiplier*Pixel.a_normalized(c0) != 1){
				triangle.setAttributeNS(null, "fill-opacity", 
						SVGUtils.svgNumber(alphaMultiplier*Pixel.a_normalized(c0)));
			}
		}
	}
	
	private static int getSubdivisionEdge(double x0,double y0, double x1,double y1, double x2,double y2){
		double e0 = Math.hypot(x0-x1, y0-y1);
		double e1 = Math.hypot(x2-x1, y2-y1);
		double e2 = Math.hypot(x2-x0, y2-y0);
		double max = e0;
		int edge = 0;
		if(e1 > max){
			max = e1;
			edge = 1;
		}
		if(e2 > max){
			max = e2;
			edge = 2;
		}
		if(10 > max){
			edge = 3;
		}
		return edge;
	}
	
	private static boolean triangleMayContain(double x0,double y0, double x1,double y1, double x2,double y2, Rectangle2D rect){
		double minx = Math.min(Math.min(x0, x1), x2);
		double miny = Math.min(Math.min(y0, y1), y2);
		double maxx = Math.max(Math.max(x0, x1), x2);
		double maxy = Math.max(Math.max(y0, y1), y2);
		return new Rectangle2D.Double(minx, miny, maxx-minx, maxy-miny).intersects(rect);
	}
	
	private static int maxARGBChannelDiff(int c1, int c2){
		int max = 0;
		int diff;
		if((diff=Math.abs(Pixel.a(c1)-Pixel.a(c2)))>max){
			max = diff;
		}
		if((diff=Math.abs(Pixel.r(c1)-Pixel.r(c2)))>max){
			max = diff;
		}
		if((diff=Math.abs(Pixel.g(c1)-Pixel.g(c2)))>max){
			max = diff;
		}
		if((diff=Math.abs(Pixel.b(c1)-Pixel.b(c2)))>max){
			max = diff;
		}
		return max;
	}
	
}
