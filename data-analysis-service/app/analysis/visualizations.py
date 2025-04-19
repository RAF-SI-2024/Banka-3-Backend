import plotly.graph_objects as go
import plotly.express as px
from plotly.subplots import make_subplots
import pandas as pd

def create_credit_score_visualization(credit_score_data):
    """Create visualization for credit score analysis"""
    if not credit_score_data:
        return None

    # Create a gauge chart for the credit score
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=credit_score_data['credit_score'],
        domain={'x': [0, 1], 'y': [0, 1]},
        title={'text': "Credit Score"},
        gauge={
            'axis': {'range': [300, 850]},
            'bar': {'color': "darkblue"},
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

    # Create a bar chart for score components
    components_fig = go.Figure(data=[
        go.Bar(
            x=list(credit_score_data['components'].keys()),
            y=list(credit_score_data['components'].values()),
            text=[f"{v:.2f}" for v in credit_score_data['components'].values()],
            textposition='auto',
        )
    ])
    components_fig.update_layout(
        title="Credit Score Components",
        xaxis_title="Component",
        yaxis_title="Score",
        yaxis_range=[0, 1]
    )

    return {
        'gauge': fig.to_html(full_html=False),
        'components': components_fig.to_html(full_html=False)
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
        name='Value Metrics'
    ))

    fig.update_layout(
        polar=dict(
            radialaxis=dict(
                visible=True,
                range=[0, 1]
            )
        ),
        showlegend=False,
        title="Client Value Metrics"
    )

    # Create a gauge for total score
    gauge_fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=client_value_data['total_score'],
        domain={'x': [0, 1], 'y': [0, 1]},
        title={'text': "Total Value Score"},
        gauge={
            'axis': {'range': [0, 1]},
            'bar': {'color': "darkblue"},
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

    return {
        'radar': fig.to_html(full_html=False),
        'gauge': gauge_fig.to_html(full_html=False)
    }

def create_churn_risk_visualization(churn_data):
    """Create visualization for churn risk analysis"""
    if not churn_data:
        return None

    # Create a gauge for risk score
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=churn_data['risk_score'],
        domain={'x': [0, 1], 'y': [0, 1]},
        title={'text': "Churn Risk Score"},
        gauge={
            'axis': {'range': [0, 1]},
            'bar': {'color': "darkblue"},
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

    # Create a bar chart for risk indicators
    indicators_fig = go.Figure(data=[
        go.Bar(
            x=list(churn_data['indicators'].keys()),
            y=[1 if v else 0 for v in churn_data['indicators'].values()],
            text=[str(v) for v in churn_data['indicators'].values()],
            textposition='auto',
        )
    ])
    indicators_fig.update_layout(
        title="Risk Indicators",
        xaxis_title="Indicator",
        yaxis_title="Present",
        yaxis_range=[0, 1]
    )

    return {
        'gauge': fig.to_html(full_html=False),
        'indicators': indicators_fig.to_html(full_html=False)
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
        hole=.3
    )])
    fig.update_layout(title="Product Usage Combinations")

    # Create bar chart for usage stats
    stats = usage_data['usage_stats']
    stats_fig = go.Figure()
    stats_fig.add_trace(go.Bar(
        x=['Total Clients', 'Clients with Cards', 'Active Accounts'],
        y=[stats['total_clients'], 
           stats['card_usage']['clients_with_cards'], 
           stats['payment_activity']['active_accounts']],
        name='Counts'
    ))
    stats_fig.update_layout(
        title="Product Usage Statistics",
        yaxis_title="Count"
    )

    return {
        'combinations': fig.to_html(full_html=False),
        'stats': stats_fig.to_html(full_html=False)
    }

def create_client_segments_visualization(segments_data):
    """Create visualization for client segmentation analysis"""
    if not segments_data:
        return None

    # Create a bar chart for cluster statistics
    clusters_df = pd.DataFrame(segments_data['clusters']).T
    clusters_fig = go.Figure()
    
    for column in clusters_df.columns:
        clusters_fig.add_trace(go.Bar(
            x=clusters_df.index,
            y=clusters_df[column],
            name=column.replace('_', ' ').title(),
            text=[f"{v:.2f}" for v in clusters_df[column]],
            textposition='auto',
        ))

    clusters_fig.update_layout(
        title="Cluster Statistics",
        xaxis_title="Cluster",
        yaxis_title="Value",
        barmode='group'
    )

    # Create a pie chart for segment distribution
    segment_counts = pd.DataFrame(segments_data['client_segments'])['cluster'].value_counts()
    distribution_fig = go.Figure(data=[go.Pie(
        labels=[f"Segment {i}" for i in segment_counts.index],
        values=segment_counts.values,
        hole=.3
    )])
    distribution_fig.update_layout(title="Client Segment Distribution")

    return {
        'clusters': clusters_fig.to_html(full_html=False),
        'distribution': distribution_fig.to_html(full_html=False)
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
            title="Loan Recommendations",
            xaxis_title="Loan Type",
            yaxis_title="Maximum Amount",
            yaxis_tickprefix="$"
        )

        # Create a gauge for overall eligibility
        eligibility_score = max(rec['confidence'] for rec in recommendations) if recommendations else 0
        eligibility_fig = go.Figure(go.Indicator(
            mode="gauge+number",
            value=eligibility_score,
            domain={'x': [0, 1], 'y': [0, 1]},
            title={'text': "Overall Eligibility Score"},
            gauge={
                'axis': {'range': [0, 1]},
                'bar': {'color': "darkblue"},
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

        return {
            'recommendations': rec_fig.to_html(full_html=False),
            'eligibility': eligibility_fig.to_html(full_html=False)
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