import plotly.graph_objects as go
import plotly.express as px
import pandas as pd


def create_churn_risk_visualization(churn_data):
    """Create visualization for churn risk assessment"""
    if not churn_data:
        return None

    # Create gauge chart for risk score
    gauge_fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=churn_data['risk_score'] * 100,  # Convert to percentage
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},
        title={'text': "Churn Risk Score", 'font': {'size': 24}},
        number={'suffix': "%", 'font': {'size': 40}},
        gauge={
            'axis': {'range': [0, 100], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [0, 30], 'color': "green"},
                {'range': [30, 70], 'color': "yellow"},
                {'range': [70, 100], 'color': "red"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': churn_data['risk_score'] * 100
            }
        }
    ))
    gauge_fig.update_layout(
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create bar chart for risk components
    components = churn_data['risk_components']
    risk_fig = go.Figure([
        go.Bar(
            x=list(components.keys()),
            y=[v * 100 for v in components.values()],  # Convert to percentage
            marker_color=['red' if v > 0.7 else 'yellow' if v > 0.3 else 'green' for v in components.values()]
        )
    ])

    risk_fig.update_layout(
        title={
            'text': "Risk Components",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        xaxis_title="Risk Component",
        yaxis_title="Risk Level (%)",
        yaxis={'range': [0, 100]},
        height=350,
        margin=dict(t=50, b=100, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Rotate x-axis labels for better readability
    risk_fig.update_xaxes(tickangle=45)

    return {
        'gauge': gauge_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'indicators': risk_fig.to_html(full_html=False, config={'displayModeBar': False})
    }