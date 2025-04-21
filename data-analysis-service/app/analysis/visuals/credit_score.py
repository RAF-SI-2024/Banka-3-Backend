import plotly.graph_objects as go


def create_credit_score_visualization(credit_score_data):
    """Create visualization for credit score analysis"""
    if not credit_score_data:
        return None

    # Create a gauge chart for the credit score
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=credit_score_data['credit_score'],
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},  # Adjust domain to make gauge smaller
        title={'text': "Credit Score", 'font': {'size': 24}},
        number={'font': {'size': 40}},
        gauge={
            'axis': {'range': [300, 850], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [300, 580], 'color': "red"},
                {'range': [580, 670], 'color': "orange"},
                {'range': [670, 750], 'color': "yellow"},
                {'range': [750, 850], 'color': "green"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': credit_score_data['credit_score']
            }
        }
    ))

    fig.update_layout(
        height=350,  # Reduce height
        margin=dict(t=50, b=0, l=25, r=25),  # Adjust margins
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create a bar chart for score components
    components_fig = go.Figure(data=[
        go.Bar(
            x=list(credit_score_data['components'].keys()),
            y=list(credit_score_data['components'].values()),
            text=[f"{v:.2f}" for v in credit_score_data['components'].values()],
            textposition='auto',
            marker_color='rgb(82, 106, 255)'
        )
    ])
    components_fig.update_layout(
        title={
            'text': "Credit Score Components",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        xaxis_title="Component",
        yaxis_title="Score",
        yaxis_range=[0, 1],
        height=350,  # Match height with gauge
        margin=dict(t=50, b=100, l=50, r=25),
        xaxis_tickangle=-45,
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'  # Light gray background for the plot
    )

    return {
        'gauge': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'components': components_fig.to_html(full_html=False, config={'displayModeBar': False})
    }
