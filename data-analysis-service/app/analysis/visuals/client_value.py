import plotly.graph_objects as go


def create_client_value_visualization(client_value_data):
    """Create visualization for client value analysis"""
    if not client_value_data:
        return None

    # Create a radar chart for value metrics
    fig = go.Figure()

    fig.add_trace(go.Scatterpolar(
        r=list(client_value_data['metrics'].values()),
        theta=list(client_value_data['metrics'].keys()),
        fill='toself',
        name='Value Metrics',
        line=dict(color='rgb(82, 106, 255)')
    ))

    fig.update_layout(
        polar=dict(
            radialaxis=dict(
                visible=True,
                range=[0, 1]
            )
        ),
        showlegend=False,
        title={
            'text': "Client Value Metrics",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create a gauge for total score
    gauge_fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=client_value_data['total_score'],
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},
        title={'text': "Total Value Score", 'font': {'size': 24}},
        number={'font': {'size': 40}},
        gauge={
            'axis': {'range': [0, 1], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [0, 0.4], 'color': "red"},
                {'range': [0.4, 0.8], 'color': "yellow"},
                {'range': [0.8, 1], 'color': "green"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': client_value_data['total_score']
            }
        }
    ))
    gauge_fig.update_layout(
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    return {
        'radar': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'gauge': gauge_fig.to_html(full_html=False, config={'displayModeBar': False})
    }