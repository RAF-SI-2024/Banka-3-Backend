from sqlalchemy.orm import Session
from sqlalchemy import select, func
from datetime import datetime
from ..models import Account, Payment, Card


class ClientValueAnalysis:
    def __init__(self, db: Session):
        self.db = db

    def calculate_client_value(self, client_id: int):
        """Calculate the lifetime value of a client"""
        # Get account and transaction history
        query = select(
            Account.balance,
            func.count(Payment.id).label('transaction_count'),
            func.sum(Payment.amount).label('total_transaction_amount'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count'),
            func.min(Account.creation_date).label('first_activity')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance)

        result = self.db.execute(query).first()

        if not result:
            return None

        days_active = (datetime.now() - datetime.combine(result.first_activity,
                                                         datetime.min.time())).days if result.first_activity else 0

        # Calculate various value metrics
        value_metrics = {
            'balance_score': min(float(result.balance) / 10000, 1.0) if result.balance else 0,
            'transaction_score': min(result.transaction_count / 100, 1.0),
            'amount_score': min(float(result.total_transaction_amount) / 100000,
                                1.0) if result.total_transaction_amount else 0,
            'product_score': min(result.card_count / 2, 1.0),
            'loyalty_score': min(days_active / 365, 1.0)
        }

        # Calculate weighted average for final score
        weights = {
            'balance_score': 0.3,
            'transaction_score': 0.2,
            'amount_score': 0.2,
            'product_score': 0.15,
            'loyalty_score': 0.15
        }

        total_score = sum(score * weights[metric] for metric, score in value_metrics.items())

        return {
            'client_id': client_id,
            'total_score': round(total_score, 2),
            'metrics': {k: round(v, 2) for k, v in value_metrics.items()},
            'segment': 'Premium' if total_score > 0.8 else 'Standard' if total_score > 0.4 else 'Basic'
        }
