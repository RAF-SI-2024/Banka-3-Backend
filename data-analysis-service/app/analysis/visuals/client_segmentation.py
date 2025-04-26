import plotly.graph_objects as go
import plotly.express as px
import pandas as pd


def create_client_segments_visualization(segments_data):
    """Create visualization for client segmentation analysis"""
    if not segments_data:
        return None

    # Extract data
    clusters = segments_data['clusters']
    characteristics = segments_data['segment_characteristics']

    # Create a DataFrame for easier plotting
    df = pd.DataFrame.from_dict(clusters, orient='index')

    # 1. Segment Size Distribution
    size_fig = go.Figure(data=[
        go.Pie(
            labels=[f"Segment {i}" for i in range(len(characteristics))],
            values=[char['size'] for char in characteristics.values()],
            hole=0.3,
            textinfo='label+percent',
            textposition='outside',
            marker=dict(colors=px.colors.qualitative.Pastel)
        )
    ])
    size_fig.update_layout(
        title="Client Segment Distribution",
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 2. Balance and Activity Analysis
    balance_activity_fig = go.Figure()

    # Add balance bars
    balance_activity_fig.add_trace(go.Bar(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['balance'],
        name='Average Balance',
        marker_color='rgb(82, 106, 255)'
    ))

    # Add activity line
    balance_activity_fig.add_trace(go.Scatter(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['activity_level'],
        name='Activity Level',
        yaxis='y2',
        line=dict(color='rgb(255, 127, 14)')
    ))

    balance_activity_fig.update_layout(
        title="Balance and Activity by Segment",
        yaxis=dict(title="Average Balance"),
        yaxis2=dict(
            title="Activity Level",
            overlaying="y",
            side="right"
        ),
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 3. Card Usage Analysis
    card_usage_fig = go.Figure()

    # Add card count bars
    card_usage_fig.add_trace(go.Bar(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['card_count'],
        name='Total Cards',
        marker_color='rgb(44, 160, 44)'
    ))

    # Add credit card ratio line
    card_usage_fig.add_trace(go.Scatter(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['credit_card_ratio'],
        name='Credit Card Ratio',
        yaxis='y2',
        line=dict(color='rgb(214, 39, 40)')
    ))

    card_usage_fig.update_layout(
        title="Card Usage by Segment",
        yaxis=dict(title="Total Cards"),
        yaxis2=dict(
            title="Credit Card Ratio",
            overlaying="y",
            side="right"
        ),
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 4. Transaction Analysis
    transaction_fig = go.Figure()

    # Add transaction count bars
    transaction_fig.add_trace(go.Bar(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['transaction_count'],
        name='Transaction Count',
        marker_color='rgb(148, 103, 189)'
    ))

    # Add success rate line
    transaction_fig.add_trace(go.Scatter(
        x=[f"Segment {i}" for i in range(len(characteristics))],
        y=df['transaction_success_rate'],
        name='Success Rate',
        yaxis='y2',
        line=dict(color='rgb(140, 86, 75)')
    ))

    transaction_fig.update_layout(
        title="Transaction Analysis by Segment",
        yaxis=dict(title="Transaction Count"),
        yaxis2=dict(
            title="Success Rate",
            overlaying="y",
            side="right"
        ),
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # 5. Segment Characteristics Table
    characteristics_df = pd.DataFrame.from_dict(characteristics, orient='index')
    characteristics_fig = go.Figure(data=[go.Table(
        header=dict(
            values=['Segment'] + list(characteristics_df.columns),
            fill_color='paleturquoise',
            align='left'
        ),
        cells=dict(
            values=[['Segment ' + str(i) for i in range(len(characteristics))]] +
                   [characteristics_df[col] for col in characteristics_df.columns],
            fill_color='lavender',
            align='left'
        )
    )])
    characteristics_fig.update_layout(
        title="Segment Characteristics",
        height=400,
        margin=dict(t=50, b=0, l=0, r=0)
    )

    # Combine all visualizations
    html_content = f"""
    <div class="container">
        <div class="card">
            {size_fig.to_html(full_html=False, include_plotlyjs=False)}
                </div>
        <div class="card">
            {balance_activity_fig.to_html(full_html=False, include_plotlyjs=False)}
            </div>
        <div class="card">
            {card_usage_fig.to_html(full_html=False, include_plotlyjs=False)}
        </div>
        <div class="card">
            {transaction_fig.to_html(full_html=False, include_plotlyjs=False)}
        </div>
        <div class="card" style="grid-column: span 2;">
            {characteristics_fig.to_html(full_html=False, include_plotlyjs=False)}
            </div>
        </div>
    """

    return html_content
