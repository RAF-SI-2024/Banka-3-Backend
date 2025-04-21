from sqlalchemy.orm import Session
from sqlalchemy import select, func, case
from datetime import datetime
from app.analysis.models import Account, Payment, Card, Loan, PaymentStatus
from typing import List, Dict, Any


class CreditScoring:
    def __init__(self, db: Session):
        self.db = db

    def calculate_credit_score(self, client_id: int):
        """Calculate an internal credit score for a client"""
        # Get comprehensive client history
        query = select(
            Account.balance,
            Account.creation_date,
            func.count(Payment.id).label('total_transactions'),
            func.count(case((Payment.status == PaymentStatus.COMPLETED, Payment.id))).label('successful_transactions'),
            func.avg(Payment.amount).label('avg_transaction_amount'),
            func.count(Card.id).label('card_count')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .outerjoin(Card, Account.account_number == Card.account_number) \
            .where(Account.client_id == client_id) \
            .group_by(Account.balance, Account.creation_date)

        result = self.db.execute(query).first()

        if not result:
            return None

        # Calculate various scoring components
        days_active = (datetime.now() - datetime.combine(result.creation_date,
                                                         datetime.min.time())).days if result.creation_date else 0

        score_components = {
            'balance_score': min(float(result.balance) / 10000, 1.0) if result.balance else 0,
            'transaction_reliability': result.successful_transactions / result.total_transactions if result.total_transactions > 0 else 0,
            'activity_score': min(result.total_transactions / 100, 1.0),
            'average_amount_score': min(float(result.avg_transaction_amount) / 1000,
                                        1.0) if result.avg_transaction_amount else 0,
            'product_diversity': min(result.card_count / 2, 1.0),
            'account_age_score': min(days_active / 365, 1.0)
        }

        # Calculate weighted score
        weights = {
            'balance_score': 0.25,
            'transaction_reliability': 0.2,
            'activity_score': 0.15,
            'average_amount_score': 0.15,
            'product_diversity': 0.1,
            'account_age_score': 0.15
        }

        total_score = sum(score * weights[component] for component, score in score_components.items())

        # Convert to 300-850 scale (typical credit score range)
        credit_score = int(300 + (total_score * 550))

        return {
            'client_id': client_id,
            'credit_score': credit_score,
            'components': {k: round(v, 2) for k, v in score_components.items()},
            'rating': 'Excellent' if credit_score >= 750
            else 'Good' if credit_score >= 670
            else 'Fair' if credit_score >= 580
            else 'Poor'
        }

    def get_client_loan_stats(self) -> List[Dict[str, Any]]:
        """Get loan statistics for each client."""
        client_stats = self.db.execute(
            select(
                Account.client_id,
                func.count(Loan.id).label("loan_count"),
                func.sum(Loan.amount).label("total_loan_amount"),
                func.avg(Loan.amount).label("avg_loan_amount")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .group_by(Account.client_id)
        ).all()

        return [
            {
                "client_id": stat.client_id,
                "loan_count": stat.loan_count or 0,
                "total_loan_amount": float(stat.total_loan_amount or 0),
                "avg_loan_amount": float(stat.avg_loan_amount or 0)
            }
            for stat in client_stats
        ]

    def get_loan_analysis(self) -> Dict[str, Any]:
        """Get comprehensive loan analysis."""
        # Total loans and amounts
        loan_stats = self.db.execute(
            select(
                func.count(Loan.id).label("total_loans"),
                func.sum(Loan.amount).label("total_amount"),
                func.avg(Loan.amount).label("avg_amount"),
                func.avg(Loan.nominal_interest_rate).label("avg_nominal_interest_rate"),
                func.avg(Loan.effective_interest_rate).label("avg_effective_interest_rate")
            )
        ).first()

        # Loans by type
        loans_by_type = self.db.execute(
            select(
                Loan.type,
                func.count(Loan.id).label("count"),
                func.sum(Loan.amount).label("total_amount"),
                func.avg(Loan.nominal_interest_rate).label("avg_nominal_interest_rate"),
                func.avg(Loan.effective_interest_rate).label("avg_effective_interest_rate"),
                func.avg(Loan.repayment_period).label("avg_repayment_period")
            )
            .group_by(Loan.type)
        ).all()

        # Loans by status
        loans_by_status = self.db.execute(
            select(
                Loan.status,
                func.count(Loan.id).label("count"),
                func.sum(Loan.amount).label("total_amount")
            )
            .group_by(Loan.status)
        ).all()

        # Average loan amount by client segment
        avg_loan_by_segment = self.db.execute(
            select(
                Account.client_id,
                func.avg(Loan.amount).label("avg_loan_amount")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .group_by(Account.client_id)
        ).all()

        return {
            "total_loans": loan_stats.total_loans or 0,
            "total_amount": float(loan_stats.total_amount or 0),
            "avg_amount": float(loan_stats.avg_amount or 0),
            "avg_nominal_interest_rate": float(loan_stats.avg_nominal_interest_rate or 0),
            "avg_effective_interest_rate": float(loan_stats.avg_effective_interest_rate or 0),
            "loans_by_type": [
                {
                    "type": loan_type.type.value,
                    "count": loan_type.count,
                    "total_amount": float(loan_type.total_amount or 0),
                    "avg_nominal_interest_rate": float(loan_type.avg_nominal_interest_rate or 0),
                    "avg_effective_interest_rate": float(loan_type.avg_effective_interest_rate or 0),
                    "avg_repayment_period": float(loan_type.avg_repayment_period or 0)
                }
                for loan_type in loans_by_type
            ],
            "loans_by_status": [
                {
                    "status": loan_status.status.value,
                    "count": loan_status.count,
                    "total_amount": float(loan_status.total_amount or 0)
                }
                for loan_status in loans_by_status
            ],
            "avg_loan_by_segment": [
                {
                    "client_id": segment.client_id,
                    "avg_loan_amount": float(segment.avg_loan_amount or 0)
                }
                for segment in avg_loan_by_segment
            ]
        }

    def get_loan_recommendations(self, client_id: int) -> Dict[str, Any]:
        """Get loan recommendations for a specific client."""
        # Get client's financial profile
        client_profile = self.db.execute(
            select(
                Account.client_id,
                func.count(Loan.id).label("existing_loans"),
                func.sum(Loan.amount).label("total_loan_amount"),
                func.avg(Loan.amount).label("avg_loan_amount"),
                func.avg(Account.balance).label("avg_balance"),
                func.count(Payment.id).label("payment_count"),
                func.avg(Payment.amount).label("avg_payment_amount")
            )
            .outerjoin(Loan, Account.account_number == Loan.account_account_number)
            .outerjoin(Payment, Account.account_number == Payment.sender_account_number)
            .where(Account.client_id == client_id)
            .group_by(Account.client_id)
        ).first()

        if not client_profile:
            return {"error": "Client not found"}

        # Calculate recommendation score
        recommendation_score = 0
        recommended_loan_types = []

        # Check if client qualifies for premium loans
        if (client_profile.avg_balance or 0) > 10000 and (client_profile.payment_count or 0) > 10:
            recommendation_score += 2
            recommended_loan_types.append("PREMIUM")

        # Check if client qualifies for personal loans
        if (client_profile.avg_balance or 0) > 5000 and (client_profile.payment_count or 0) > 5:
            recommendation_score += 1
            recommended_loan_types.append("PERSONAL")

        # Check if client qualifies for credit card
        if (client_profile.avg_balance or 0) > 2000 and (client_profile.payment_count or 0) > 3:
            recommendation_score += 1
            recommended_loan_types.append("CREDIT_CARD")

        return {
            "client_id": client_id,
            "recommendation_score": recommendation_score,
            "recommended_loan_types": recommended_loan_types,
            "financial_profile": {
                "existing_loans": client_profile.existing_loans or 0,
                "total_loan_amount": float(client_profile.total_loan_amount or 0),
                "avg_loan_amount": float(client_profile.avg_loan_amount or 0),
                "avg_balance": float(client_profile.avg_balance or 0),
                "payment_count": client_profile.payment_count or 0,
                "avg_payment_amount": float(client_profile.avg_payment_amount or 0)
            }
        }
