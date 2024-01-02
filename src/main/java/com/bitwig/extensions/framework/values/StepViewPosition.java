package com.bitwig.extensions.framework.values;

import com.bitwig.extension.controller.api.Clip;

public class StepViewPosition {

    private double gridResolution;
    private double loopLength = 0.0;
    private int pagePosition = 0;
    private int pages = 0;
    private int steps;
    private int stepsPerPage;

    private final Clip clip;
    private final BooleanValueObject canScrollLeft = new BooleanValueObject();
    private final BooleanValueObject canScrollRight = new BooleanValueObject();
    private final String name;

    public StepViewPosition(final Clip clip, int stepsPerPage, String name) {
        super();
        this.clip = clip;
        gridResolution = 0.25;
        this.name = name;
        this.stepsPerPage = stepsPerPage;
        this.clip.setStepSize(gridResolution);
        clip.getLoopLength().addValueObserver(this::handleLoopLengthChanged);
        clip.scrollToStep(pagePosition * stepsPerPage);
    }

    public double lengthWithLastStep(final int index) {
        return gridResolution * (pagePosition * 32 + index + 1);
    }

    public void setStepsPerPage(int stepsPerPage) {
        this.stepsPerPage = stepsPerPage;
        steps = (int) (this.loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / stepsPerPage + 1;
        updateStates();
    }

    public void handleLoopLengthChanged(final double newLength) {
        this.loopLength = newLength;
        steps = (int) (this.loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / stepsPerPage + 1;
        updateStates();
    }

    public int getCurrentPage() {
        return pagePosition;
    }

    public int getAvailableSteps() {
        return Math.max(0, steps - pagePosition * stepsPerPage);
    }

    public int getPages() {
        return pages;
    }

    public void setGridResolution(final double resolution) {
        final double quote = this.gridResolution / resolution;
        gridResolution = resolution;
        this.clip.setStepSize(gridResolution);
        pagePosition = (int) (pagePosition * quote);
        steps = (int) (this.loopLength / gridResolution);
        pages = Math.max(0, steps - 1) / stepsPerPage + 1;
        clip.scrollToStep(pagePosition * stepsPerPage);

        updateStates();
    }

    private void updateStates() {
        if (pagePosition < pages) {
            clip.scrollToStep(pagePosition * stepsPerPage);
        }
        canScrollLeft.set(pagePosition > 0);
        canScrollRight.set(pagePosition < pages - 1);
    }

    public BooleanValueObject canScrollLeft() {
        return canScrollLeft;
    }

    public BooleanValueObject canScrollRight() {
        return canScrollRight;
    }

    public void setPage(final int index) {
        this.pagePosition = index;
        updateStates();
    }

    public int getStepOffset() {
        return pagePosition * stepsPerPage;
    }

    public double getPosition() {
        return pagePosition * gridResolution;
    }

    public double getGridResolution() {
        return gridResolution;
    }

    public void setLoopLength(final double loopLength) {
        this.loopLength = loopLength;
    }

    public double getLoopLength() {
        return loopLength;
    }

    public void scrollLeft() {
        if (pagePosition > 0) {
            pagePosition--;
            clip.scrollToStep(pagePosition * stepsPerPage);
            updateStates();
        }
    }

    public void scrollRight() {
        if (pagePosition < pages) {
            pagePosition++;
            clip.scrollToStep(pagePosition * stepsPerPage);
            updateStates();
        }
    }

}
