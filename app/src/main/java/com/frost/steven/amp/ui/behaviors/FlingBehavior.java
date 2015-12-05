package com.frost.steven.amp.ui.behaviors;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class FlingBehavior extends AppBarLayout.Behavior
{
    private static final int FLING_THRESHOLD = 3;
    private boolean m_isPositive;

    public FlingBehavior() {}

    public FlingBehavior(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinator, AppBarLayout appBar, View view, float vX, float vY, boolean consumed)
    {
        if (vY > 0 && !m_isPositive || vY < 0 && m_isPositive)
        {
            vY = vY * -1;
        }

        if (view instanceof RecyclerView && vY < 0)
        {
            final RecyclerView recyclerView = (RecyclerView) view;
            final View firstChild = recyclerView.getChildAt(0);
            final int childAdapterPosition = recyclerView.getChildAdapterPosition(firstChild);

            consumed = childAdapterPosition > FLING_THRESHOLD;
        }
        return super.onNestedFling(coordinator, appBar, view, vX, vY, consumed);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinator, AppBarLayout appBar, View target, int dX, int dY, int[] consumed)
    {
        super.onNestedPreScroll(coordinator, appBar, target, dX, dY, consumed);

        m_isPositive = dY > 0;
    }
}