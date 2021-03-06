package org.tepi.imageviewer.client;

import org.tepi.imageviewer.client.imagepreloader.FitImage;
import org.tepi.imageviewer.client.imagepreloader.FitImageLoadEvent;
import org.tepi.imageviewer.client.imagepreloader.FitImageLoadHandler;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.vaadin.client.BrowserInfo;

/**
 * VImage is a widget extending the FlowPanel. A VImage instance contains one
 * FitImage and the loading indicator element for the image. VImage instances
 * are designed to be used in the ImageViewer add-on.
 * 
 * @author Teppo Kurki
 */
class VImage extends FlowPanel {
	/** Style name for the VImage widget */
	private static final String CLASSNAME = "v-image";
	/** Style name for the actual image element inside the VImage */
	private static final String IMAGEELEMENT = "fitimage";

	/** Reference to owner */
	private VImageViewer owner;

	/** Horizontal margin around the image element */
	private int horizontalMargin;
	/** Vertical margin around the image element */
	private int verticalMargin;

	/** Image element */
	private FitImage image;
	/** Panel for the loading indicator image */
	private FlowPanel loading;
	/** Image index (in relation to the set of visible images) */
	private int index;
	/** Enable mouse over effects */
	private boolean mouseOverEffects;
	/** Is image the center one */
	private boolean center;
	/** Is image maximized (or the only visible one) */
	private boolean maximized;

	/* Current size and horizontal position of the image container */
	private int currentWidth;
	private int currentHeight;
	private int currentX;

	/* Animation helper properties; start and end position and width */
	private int startWidth;
	private int endWidth;
	private int startPosition;
	private int endPosition;

	VImage() {
		setStyleName(CLASSNAME);
		Style style = getElement().getStyle();
		style.setPosition(Position.ABSOLUTE);

		/* Create loading panel */
		loading = new FlowPanel();
		loading.addStyleName("image-loading");
		style = loading.getElement().getStyle();
		style.setPosition(Position.ABSOLUTE);
		style.setWidth(100, Unit.PCT);
		style.setHeight(100, Unit.PCT);

		add(loading);
	}

	/**
	 * Sets image source URI, creates image and attaches click and load
	 * handlers.
	 * 
	 * @param uri
	 *            URI pointing to the image to show
	 */
	void setImageSource(String uri) {
		if (uri == null) {
			return;
		}
		/* Create image */
		image = new FitImage();

		/* Better image interpolation mode for IE */
		if (BrowserInfo.get().isIE()) {
			Style style = image.getElement().getStyle();
			style.setProperty("-ms-interpolation-mode", "bicubic");
		}

		/* Forward image clicks to VImageViewer class */
		image.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				owner.imageClicked(index);
			}
		});

		/* Mouse over effects */
		if (mouseOverEffects && !maximized) {
			image.addMouseOverHandler(new MouseOverHandler() {
				public void onMouseOver(MouseOverEvent event) {
					Style style = image.getElement().getStyle();
					style.setOpacity(1.0);
					style.setProperty("filter", "alpha(opacity = 100)");
				}
			});
			image.addMouseOutHandler(new MouseOutHandler() {
				public void onMouseOut(MouseOutEvent event) {
					Style style = image.getElement().getStyle();
					if (!center) {
						style.setOpacity(0.7);
						style.setProperty("filter", "alpha(opacity = 70)");
					} else {
						style.setOpacity(0.9);
						style.setProperty("filter", "alpha(opacity = 90)");
					}
				}
			});
		}

		/* Image styling: no borders; hide the image initially */
		Style style = image.getElement().getStyle();
		style.setBorderStyle(BorderStyle.NONE);
		style.setPosition(Position.ABSOLUTE);
		style.setVisibility(Visibility.HIDDEN);
		if (mouseOverEffects && !maximized) {
			if (!center) {
				style.setOpacity(0.7);
				style.setProperty("filter", "alpha(opacity = 70)");
			} else {
				style.setOpacity(0.9);
				style.setProperty("filter", "alpha(opacity = 90)");
			}
		}

		/* When Image has loaded => show image; hide the loading indicator */
		image.addFitImageLoadHandler(new FitImageLoadHandler() {
			public void imageLoaded(FitImageLoadEvent event) {
				Style style = image.getElement().getStyle();
				style.setVisibility(Visibility.VISIBLE);
				style = loading.getElement().getStyle();
				style.setVisibility(Visibility.HIDDEN);
			}
		});

		/*
		 * Another load handler to fix image size and position after it has been
		 * loaded. For some reason calling this from the FitImageLoadHandler
		 * produces animation glitches in Chrome.
		 */
		image.addLoadHandler(new LoadHandler() {
			public void onLoad(LoadEvent event) {
				fixImageSizeAndPosition();
			}
		});
		// TODO: Fix the glitch in Safari: The chrome-fix did not help

		image.setUrl(uri);
		image.setStyleName(IMAGEELEMENT);

		add(image);
	}

	/**
	 * Fixes image element sizing and positioning within its container
	 */
	void fixImageSizeAndPosition() {
		if (image == null) {
			return;
		}
		/* Fix height */
		int h = currentHeight - 2 * verticalMargin;
		image.setMaxHeight(h >= 0 ? h : 0);
		/* Fix width */
		int w = currentWidth - 2 * horizontalMargin;
		image.setMaxWidth(w >= 0 ? w : 0);
		/* Set vertical margin as distance from bottom */
		Style style = image.getElement().getStyle();
		style.setBottom(verticalMargin, Unit.PX);
		/* Set horizontal position */
		int imgWidth = image.getWidth();
		int left = 0;
		if (imgWidth > 0) {
			if (imgWidth < currentWidth - 2 * horizontalMargin) {
				left = (int) (Math.floor((currentWidth - imgWidth) / 2));
			} else {
				left = horizontalMargin;
			}
			if (!style.getLeft().equals(left + "px")) {
				style.setLeft(left, Unit.PX);
			}
		}
	}

	void setCurrentX(int currentX) {
		this.currentX = currentX;
		Style style = getElement().getStyle();
		style.setLeft(currentX, Unit.PX);
	}

	void setCurrentWidth(int currentWidth) {
		this.currentWidth = currentWidth;
		Style style = getElement().getStyle();
		style.setWidth(currentWidth, Unit.PX);
	}

	void setCurrentHeight(int currentHeight) {
		this.currentHeight = currentHeight;
		Style style = getElement().getStyle();
		style.setHeight(currentHeight, Unit.PX);
	}

	void initAnimation(int endWidth, int endPosition) {
		startWidth = currentWidth;
		startPosition = currentX;
		this.endWidth = endWidth;
		this.endPosition = endPosition;
	}

	int getStartWidth() {
		return startWidth;
	}

	int getEndWidth() {
		return endWidth;
	}

	int getStartPosition() {
		return startPosition;
	}

	int getEndPosition() {
		return endPosition;
	}

	void setHorizontalMargin(int horizontalMargin) {
		this.horizontalMargin = horizontalMargin;
	}

	void setVerticalMargin(int verticalMargin) {
		this.verticalMargin = verticalMargin;
	}

	int getCurrentWidth() {
		return currentWidth;
	}

	int getCurrentX() {
		return currentX;
	}

	void setIndex(int index) {
		this.index = index;
	}

	void setOwner(VImageViewer owner) {
		this.owner = owner;
	}

	void setCenter(boolean center) {
		this.center = center;
	}

	void setMouseOverEffects(boolean mouseOverEffects) {
		this.mouseOverEffects = mouseOverEffects;
	}

	int getImageAndMarginWidth() {
		return image.getWidth() + 2 * horizontalMargin;
	}

	void setMaximized(boolean maximized) {
		this.maximized = maximized;
	}
}
