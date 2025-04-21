from datetime import datetime
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sqlalchemy import func, case, select
from sqlalchemy.orm import Session
from ..models import Account, Payment, Card, PaymentStatus, CardType, AccountType
import numpy as np
import pandas as pd


class ClientSegmentation:
    def __init__(self, db: Session):
        self.db = db

    def get_client_features(self):
        """Extract relevant features for client segmentation based on actual data patterns"""
        # Get account information with transaction statistics
        query = select(
            Account.client_id,
            Account.balance,
            Account.type.label('account_type'),
            func.count(Payment.id).label('transaction_count'),
            func.sum(Payment.amount).label('total_transaction_amount'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count'),
            func.count(case((Card.type == CardType.CREDIT, Card.id))).label('credit_cards'),
            func.count(case((Card.type == CardType.DEBIT, Card.id))).label('debit_cards'),
            func.min(Account.creation_date).label('first_activity'),
            func.count(case((Payment.status == PaymentStatus.COMPLETED, Payment.id))).label('successful_transactions'),
            func.count(case((Account.type == AccountType.FOREIGN, Account.account_number))).label('foreign_accounts')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .group_by(Account.client_id, Account.balance, Account.type)

        result = self.db.execute(query).all()

        # Convert to DataFrame for easier processing
        df = pd.DataFrame(result, columns=[
            'client_id', 'balance', 'account_type',
            'transaction_count', 'total_transaction_amount', 'avg_transaction_amount',
            'card_count', 'credit_cards', 'debit_cards',
            'first_activity', 'successful_transactions', 'foreign_accounts'
        ])

        # Replace None values with 0 for numeric columns
        numeric_columns = ['balance', 'transaction_count', 'total_transaction_amount',
                           'avg_transaction_amount', 'card_count', 'credit_cards',
                           'debit_cards', 'successful_transactions', 'foreign_accounts']
        df[numeric_columns] = df[numeric_columns].fillna(0)

        # Calculate additional metrics based on actual data patterns
        df['transaction_success_rate'] = df['successful_transactions'] / df['transaction_count'].replace(0, 1)
        df['credit_card_ratio'] = df['credit_cards'] / df['card_count'].replace(0, 1)
        df['days_active'] = (datetime.now() - pd.to_datetime(df['first_activity'])).dt.days
        df['activity_level'] = df['transaction_count'] / df['days_active'].replace(0, 1)
        df['has_foreign_account'] = df['foreign_accounts'] > 0

        # Create balance level using a simple function
        def get_balance_level(balance):
            if balance > 50000:
                return 'Very High'
            elif balance > 20000:
                return 'High'
            elif balance > 5000:
                return 'Moderate'
            elif balance > 1000:
                return 'Low'
            else:
                return 'Very Low'

        df['balance_level'] = df['balance'].apply(get_balance_level)

        # Replace NaN and inf values with 0
        df = df.replace([np.inf, -np.inf], np.nan)
        df = df.fillna(0)

        return df

    def perform_clustering(self, n_clusters=5):
        """Perform k-means clustering on client data with features matching actual patterns"""
        df = self.get_client_features()

        # Select features for clustering based on actual data patterns
        features = df[[
            'balance', 'transaction_count', 'total_transaction_amount',
            'avg_transaction_amount', 'card_count', 'credit_card_ratio',
            'transaction_success_rate', 'activity_level', 'has_foreign_account'
        ]]

        # Standardize features
        scaler = StandardScaler()
        features_scaled = scaler.fit_transform(features)

        # Perform k-means clustering
        kmeans = KMeans(n_clusters=n_clusters, random_state=42)
        df['cluster'] = kmeans.fit_predict(features_scaled)

        # Analyze clusters based on actual data patterns
        cluster_stats = df.groupby('cluster').agg({
            'balance': 'mean',
            'transaction_count': 'mean',
            'total_transaction_amount': 'mean',
            'avg_transaction_amount': 'mean',
            'card_count': 'mean',
            'credit_card_ratio': 'mean',
            'transaction_success_rate': 'mean',
            'activity_level': 'mean',
            'has_foreign_account': 'mean',
            'client_id': 'count'
        }).round(2)

        # Calculate segment characteristics based on actual data patterns
        segment_characteristics = {}
        for cluster in cluster_stats.index:
            stats = cluster_stats.loc[cluster]
            characteristics = {
                'size': int(stats['client_id']),
                'balance_level': 'Very High' if stats['balance'] > 50000 else 'High' if stats[
                                                                                            'balance'] > 20000 else 'Moderate' if
                stats['balance'] > 5000 else 'Low' if stats['balance'] > 1000 else 'Very Low',
                'activity_level': 'High' if stats['activity_level'] > 0.7 else 'Medium' if stats[
                                                                                               'activity_level'] > 0.3 else 'Low',
                'transaction_volume': 'High' if stats['total_transaction_amount'] > 0.7 else 'Medium' if stats[
                                                                                                             'total_transaction_amount'] > 0.3 else 'Low',
                'card_usage': 'Multiple' if stats['card_count'] > 2 else 'Standard' if stats[
                                                                                           'card_count'] > 0 else 'None',
                'credit_card_usage': 'High' if stats['credit_card_ratio'] > 0.7 else 'Medium' if stats[
                                                                                                     'credit_card_ratio'] > 0.3 else 'Low',
                'success_rate': 'High' if stats['transaction_success_rate'] > 0.9 else 'Medium' if stats[
                                                                                                       'transaction_success_rate'] > 0.7 else 'Low',
                'international_activity': 'Yes' if stats['has_foreign_account'] > 0.5 else 'No'
            }
            segment_characteristics[cluster] = characteristics

        return {
            'clusters': cluster_stats.to_dict('index'),
            'client_segments': df[['client_id', 'cluster']].to_dict('records'),
            'segment_characteristics': segment_characteristics
        }


def generate_segment_insights(segments_data):
    """Generate insights for each segment based on their characteristics"""
    characteristics = segments_data['segment_characteristics']
    insights = []

    for segment_id, char in characteristics.items():
        segment_insight = f"""
        <div class="segment-description">
            <h3>Segment {segment_id}</h3>
            <p>This segment represents {char['size']} clients with the following characteristics:</p>
            <div class="metrics">
                <span class="metric">Balance Level: {char['balance_level']}</span>
                <span class="metric">Activity Level: {char['activity_level']}</span>
                <span class="metric">Card Usage: {char['card_usage']}</span>
                <span class="metric">Credit Card Usage: {char['credit_card_usage']}</span>
                <span class="metric">International Activity: {char['international_activity']}</span>
            </div>
            <p><strong>Recommendations:</strong></p>
            <ul>
                {segments_data.generate_segment_recommendations(char)}
            </ul>
        </div>
        """
        insights.append(segment_insight)

    return "\n".join(insights)


def generate_segment_recommendations(characteristics):
    """Generate specific recommendations based on segment characteristics"""
    recommendations = []

    # Balance-based recommendations
    if characteristics['balance_level'] in ['High', 'Very High']:
        recommendations.append("Offer premium investment products and wealth management services")
    elif characteristics['balance_level'] in ['Low', 'Very Low']:
        recommendations.append("Consider financial education programs and basic banking solutions")

    # Activity-based recommendations
    if characteristics['activity_level'] == 'High':
        recommendations.append("Introduce transaction fee packages and cashback rewards")
    elif characteristics['activity_level'] == 'Low':
        recommendations.append("Promote mobile banking features to increase engagement")

    # Card usage recommendations
    if characteristics['card_usage'] == 'Multiple':
        recommendations.append("Offer premium credit cards with rewards programs")
    elif characteristics['card_usage'] == 'None':
        recommendations.append("Promote card-based services and digital payment solutions")

    # International activity recommendations
    if characteristics['international_activity'] == 'Yes':
        recommendations.append("Offer foreign currency accounts and international transfer services")

    # Credit card specific recommendations
    if characteristics['credit_card_usage'] == 'High':
        recommendations.append("Consider card-based insurance products and travel benefits")

    # Format recommendations as list items
    return "\n".join([f"<li>{rec}</li>" for rec in recommendations])
