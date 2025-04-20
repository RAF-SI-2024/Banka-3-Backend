import plotly.graph_objects as go
import plotly.express as px
import pandas as pd


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


def create_churn_risk_visualization(churn_data):
    """Create visualization for churn risk analysis"""
    if not churn_data:
        return None

    # Create a gauge for risk score
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=churn_data['risk_score'],
        domain={'x': [0.1, 0.9], 'y': [0, 0.9]},
        title={'text': "Churn Risk Score", 'font': {'size': 24}},
        number={'font': {'size': 40}},
        gauge={
            'axis': {'range': [0, 1], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "darkblue", 'thickness': 0.6},
            'steps': [
                {'range': [0, 0.3], 'color': "green"},
                {'range': [0.3, 0.7], 'color': "yellow"},
                {'range': [0.7, 1], 'color': "red"}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': churn_data['risk_score']
            }
        }
    ))
    fig.update_layout(
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white'
    )

    # Create a bar chart for risk indicators
    indicators_fig = go.Figure(data=[
        go.Bar(
            x=list(churn_data['indicators'].keys()),
            y=[1 if v else 0 for v in churn_data['indicators'].values()],
            text=[str(v) for v in churn_data['indicators'].values()],
            textposition='auto',
            marker_color='rgb(82, 106, 255)'
        )
    ])
    indicators_fig.update_layout(
        title={
            'text': "Risk Indicators",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        xaxis_title="Indicator",
        yaxis_title="Present",
        yaxis_range=[0, 1],
        height=350,
        margin=dict(t=50, b=100, l=50, r=25),
        xaxis_tickangle=-45,
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'
    )

    return {
        'gauge': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'indicators': indicators_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_product_usage_visualization(usage_data):
    """Create visualization for product usage analysis"""
    if not usage_data:
        return None

    # Create pie chart for product combinations
    combinations = usage_data['product_combinations']
    fig = go.Figure(data=[go.Pie(
        labels=list(combinations.keys()),
        values=list(combinations.values()),
        hole=.3,
        marker=dict(colors=px.colors.qualitative.Set3)
    )])
    fig.update_layout(
        title={
            'text': "Product Usage Combinations",
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

    # Create bar chart for usage stats
    stats = usage_data['usage_stats']
    stats_fig = go.Figure()
    stats_fig.add_trace(go.Bar(
        x=['Total Clients', 'Clients with Cards', 'Active Accounts'],
        y=[stats['total_clients'],
           stats['card_usage']['clients_with_cards'],
           stats['payment_activity']['active_accounts']],
        marker_color='rgb(82, 106, 255)'
    ))
    stats_fig.update_layout(
        title={
            'text': "Product Usage Statistics",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Count",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'
    )

    # Create loan statistics visualizations
    loan_stats = stats['loan_activity']
    
    # Total loans and amounts
    total_loans_fig = go.Figure()
    total_loans_fig.add_trace(go.Bar(
        x=['Total Loans', 'Total Amount', 'Average Amount'],
        y=[loan_stats['total_loans']['count'],
           loan_stats['total_loans']['total_amount'],
           loan_stats['total_loans']['average_amount']],
        marker_color='rgb(44, 160, 44)'
    ))
    total_loans_fig.update_layout(
        title={
            'text': "Total Loan Statistics",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Value",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)'
    )

    # Loans by type
    loans_by_type = loan_stats['by_type']
    type_fig = go.Figure()
    for loan_type in loans_by_type:
        type_fig.add_trace(go.Bar(
            x=[loan_type['type']],
            y=[loan_type['count']],
            name=loan_type['type'],
            text=[f"${loan_type['total_amount']:,.0f}"],
            textposition='auto',
            marker_color='rgb(148, 103, 189)'
        ))
    type_fig.update_layout(
        title={
            'text': "Loans by Type",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Count",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)',
        barmode='group'
    )

    # Loans by status
    loans_by_status = loan_stats['by_status']
    status_fig = go.Figure()
    for status in loans_by_status:
        status_fig.add_trace(go.Bar(
            x=[status['status']],
            y=[status['count']],
            name=status['status'],
            text=[f"${status['total_amount']:,.0f}"],
            textposition='auto',
            marker_color='rgb(255, 127, 14)'
        ))
    status_fig.update_layout(
        title={
            'text': "Loans by Status",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        yaxis_title="Count",
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)',
        barmode='group'
    )

    return {
        'combinations': fig.to_html(full_html=False, config={'displayModeBar': False}),
        'stats': stats_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'total_loans': total_loans_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'loans_by_type': type_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'loans_by_status': status_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_client_segments_visualization(segments_data):
    """Create visualization for client segmentation analysis"""
    if not segments_data:
        return None

    # Create a bar chart for cluster statistics
    clusters_df = pd.DataFrame(segments_data['clusters']).T
    clusters_fig = go.Figure()

    # Define metric descriptions for tooltips
    metric_descriptions = {
        'balance': 'Average account balance in the segment',
        'transaction_count': 'Average number of monthly transactions',
        'total_transaction_amount': 'Average total transaction amount per month',
        'avg_transaction_amount': 'Average amount per transaction',
        'card_count': 'Average number of cards per client'
    }

    # Custom color palette for better distinction between metrics
    colors = ['rgb(82, 106, 255)',  # Blue
              'rgb(255, 127, 14)',  # Orange
              'rgb(44, 160, 44)',  # Green
              'rgb(214, 39, 40)',  # Red
              'rgb(148, 103, 189)']  # Purple

    for i, column in enumerate(clusters_df.columns):
        # Format the metric name for display
        display_name = column.replace('_', ' ').title()
        description = metric_descriptions.get(column, '')

        # Create hover text with descriptions
        hover_text = [
            f"Cluster {idx}<br>" +
            f"{display_name}: {value:.2f}<br>" +
            f"Description: {description}"
            for idx, value in enumerate(clusters_df[column])
        ]

        clusters_fig.add_trace(go.Bar(
            x=[f"Segment {i}" for i in clusters_df.index],
            y=clusters_df[column],
            name=display_name,
            text=[f"{v:.2f}" for v in clusters_df[column]],
            textposition='auto',
            marker_color=colors[i % len(colors)],
            hovertext=hover_text,
            hoverinfo='text'
        ))

    clusters_fig.update_layout(
        xaxis_title="Client Segments",
        yaxis_title="Normalized Values",
        barmode='group',
        height=350,
        margin=dict(t=50, b=50, l=50, r=25),
        paper_bgcolor='white',
        plot_bgcolor='rgb(250, 250, 250)',
        showlegend=True,
        legend=dict(
            orientation="h",
            yanchor="bottom",
            y=1.02,
            xanchor="right",
            x=1
        ),
        hoverlabel=dict(
            bgcolor="white",
            font_size=12,
            font_family="Arial"
        )
    )

    # Create a pie chart for segment distribution
    segment_counts = pd.DataFrame(segments_data['client_segments'])['cluster'].value_counts()

    # Calculate percentages for hover text
    total_clients = segment_counts.sum()
    hover_text = [
        f"Segment {i}<br>" +
        f"Clients: {count}<br>" +
        f"Percentage: {(count / total_clients * 100):.1f}%"
        for i, count in zip(segment_counts.index, segment_counts.values)
    ]

    distribution_fig = go.Figure(data=[go.Pie(
        labels=[f"Segment {i}" for i in segment_counts.index],
        values=segment_counts.values,
        hole=.3,
        marker=dict(colors=px.colors.qualitative.Set3),
        hovertext=hover_text,
        hoverinfo='text',
        textinfo='percent+label'
    )])

    distribution_fig.update_layout(
        title={
            'text': "Client Segment Distribution",
            'y': 0.95,
            'x': 0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24}
        },
        height=350,
        margin=dict(t=50, b=0, l=25, r=25),
        paper_bgcolor='white',
        plot_bgcolor='white',
        hoverlabel=dict(
            bgcolor="white",
            font_size=12,
            font_family="Arial"
        )
    )

    return {
        'clusters': clusters_fig.to_html(full_html=False, config={'displayModeBar': False}),
        'distribution': distribution_fig.to_html(full_html=False, config={'displayModeBar': False})
    }


def create_loan_recommendation_visualization(loan_data):
    """Create visualization for loan recommendations"""
    if not loan_data:
        return None

    # Create a bar chart for loan recommendations
    recommendations = loan_data['recommendations']
    if recommendations:
        rec_fig = go.Figure(data=[
            go.Bar(
                x=[rec['loan_type'] for rec in recommendations],
                y=[rec['max_amount'] for rec in recommendations],
                text=[f"${rec['max_amount']:,.0f}" for rec in recommendations],
                textposition='auto',
                marker_color=[rec['confidence'] * 255 for rec in recommendations],
                marker_colorscale='RdYlGn',
                showscale=True,
                name='Maximum Amount'
            )
        ])
        rec_fig.update_layout(
            title={
                'text': "Loan Recommendations",
                'y': 0.95,
                'x': 0.5,
                'xanchor': 'center',
                'yanchor': 'top',
                'font': {'size': 24}
            },
            xaxis_title="Loan Type",
            yaxis_title="Maximum Amount",
            yaxis_tickprefix="$",
            height=350,
            margin=dict(t=50, b=50, l=50, r=25),
            paper_bgcolor='white',
            plot_bgcolor='rgb(250, 250, 250)'
        )

        # Create a gauge for overall eligibility
        eligibility_score = max(rec['confidence'] for rec in recommendations) if recommendations else 0
        eligibility_fig = go.Figure(go.Indicator(
            mode="gauge+number",
            value=eligibility_score,
            domain={'x': [0.1, 0.9], 'y': [0, 0.9]},
            title={'text': "Overall Eligibility Score", 'font': {'size': 24}},
            number={'font': {'size': 40}},
            gauge={
                'axis': {'range': [0, 1], 'tickwidth': 1, 'tickcolor': "darkblue"},
                'bar': {'color': "darkblue", 'thickness': 0.6},
                'steps': [
                    {'range': [0, 0.3], 'color': "red"},
                    {'range': [0.3, 0.7], 'color': "yellow"},
                    {'range': [0.7, 1], 'color': "green"}
                ],
                'threshold': {
                    'line': {'color': "red", 'width': 4},
                    'thickness': 0.75,
                    'value': eligibility_score
                }
            }
        ))
        eligibility_fig.update_layout(
            height=350,
            margin=dict(t=50, b=0, l=25, r=25),
            paper_bgcolor='white',
            plot_bgcolor='white'
        )

        return {
            'recommendations': rec_fig.to_html(full_html=False, config={'displayModeBar': False}),
            'eligibility': eligibility_fig.to_html(full_html=False, config={'displayModeBar': False})
        }
    return None


def create_client_insights_visualization(insights_data):
    """Create a comprehensive dashboard for all client insights"""
    if not insights_data:
        return None

    # Create a grid of visualizations
    credit_score_viz = create_credit_score_visualization(insights_data['credit_score'])
    client_value_viz = create_client_value_visualization(insights_data['client_value'])
    churn_risk_viz = create_churn_risk_visualization(insights_data['churn_risk'])
    loan_rec_viz = create_loan_recommendation_visualization(insights_data['loan_recommendations'])

    viz_html = ""

    if credit_score_viz:
        viz_html += f"""
            <div class="card">
                <h2>Credit Score</h2>
                {credit_score_viz['gauge']}
            </div>
            <div class="card">
                <h2>Score Components</h2>
                {credit_score_viz['components']}
            </div>
        """

    if client_value_viz:
        viz_html += f"""
            <div class="card">
                <h2>Value Metrics</h2>
                {client_value_viz['radar']}
            </div>
            <div class="card">
                <h2>Total Value Score</h2>
                {client_value_viz['gauge']}
            </div>
        """

    if churn_risk_viz:
        viz_html += f"""
            <div class="card">
                <h2>Churn Risk Score</h2>
                {churn_risk_viz['gauge']}
            </div>
            <div class="card">
                <h2>Risk Indicators</h2>
                {churn_risk_viz['indicators']}
            </div>
        """

    if loan_rec_viz:
        viz_html += f"""
            <div class="card">
                <h2>Loan Recommendations</h2>
                {loan_rec_viz['recommendations']}
            </div>
            <div class="card">
                <h2>Eligibility Score</h2>
                {loan_rec_viz['eligibility']}
            </div>
        """

    return viz_html
