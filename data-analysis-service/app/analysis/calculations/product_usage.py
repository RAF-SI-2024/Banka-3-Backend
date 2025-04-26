from sqlalchemy.orm import Session
from sqlalchemy import select, func, case, and_
import pandas as pd
from app.analysis.calculations.loans import LoanAnalytics
from ..models import Account, Payment, Card, Loan, PaymentStatus, CardType, AccountType


class ProductUsageAnalytics:
    def __init__(self, db: Session):
        self.db = db

    def get_product_usage_stats(self):
        """Get comprehensive product engagement analysis"""
        
        # Get total number of clients
        total_clients = self.db.execute(
            select(func.count(func.distinct(Account.client_id)))
        ).scalar()

        # Get account statistics
        account_stats = self.db.execute(
            select(
                func.count(func.distinct(Account.client_id)).label('clients_with_accounts'),
                func.count(Account.account_number).label('total_accounts'),
                func.count(case((Account.type == AccountType.FOREIGN, Account.account_number))).label(
                    'foreign_accounts'),
                func.count(case((Account.type == AccountType.CURRENT, Account.account_number))).label(
                    'current_accounts'),
                func.avg(Account.balance).label('avg_balance')
            )
        ).first()
        
        # First, check direct card count
        direct_card_count = self.db.execute(
            select(func.count(Card.id))
            .select_from(Card)
        ).scalar()
        
        # Then, check accounts with cards
        accounts_with_cards = self.db.execute(
            select(func.count(func.distinct(Account.account_number)))
            .select_from(Account)
            .join(Card)
        ).scalar()
        
        # Get detailed card statistics
        card_stats = self.db.execute(
            select(
                func.count(func.distinct(Account.client_id)).label('clients_with_cards'),
                func.count(Card.id).label('total_cards'),
                func.count(case((Card.type == CardType.CREDIT, Card.id))).label('credit_cards'),
                func.count(case((Card.type == CardType.DEBIT, Card.id))).label('debit_cards'),
                func.avg(case((Card.type == CardType.CREDIT, Card.card_limit))).label('avg_credit_limit')
            )
            .select_from(Account)
            .join(Card, Account.account_number == Card.account_number)
        ).first()
        
        raw_cards = self.db.execute(
            select(Card.id, Card.card_number, Card.account_number, Card.type)
            .limit(5)
        ).all()

        # Get payment statistics
        payment_stats = self.db.execute(
            select(
                func.count(func.distinct(Payment.sender_account_number)).label('active_accounts'),
                func.count(Payment.id).label('total_payments'),
                func.avg(Payment.amount).label('avg_payment_amount'),
                func.sum(case((Payment.status == PaymentStatus.COMPLETED, Payment.amount))).label(
                    'total_completed_amount'),
                func.count(case((Payment.status == PaymentStatus.FAILED, Payment.id))).label('failed_payments')
            )
        ).first()

        # Get loan statistics
        loan_analytics = LoanAnalytics(self.db)
        loan_stats = loan_analytics.get_loan_statistics()

        # Calculate product correlations
        correlation_stats = self.db.execute(
            select(
                func.count(func.distinct(case((
                    and_(
                        Account.client_id.isnot(None),
                        Card.id.isnot(None)
                    ), Account.client_id
                )))).label('accounts_with_cards'),
                func.count(func.distinct(case((
                    and_(
                        Account.client_id.isnot(None),
                        Loan.id.isnot(None)
                    ), Account.client_id
                )))).label('accounts_with_loans'),
                func.count(func.distinct(case((
                    and_(
                        Card.id.isnot(None),
                        Loan.id.isnot(None)
                    ), Account.client_id
                )))).label('cards_with_loans'),
                func.count(func.distinct(case((
                    and_(
                        Account.client_id.isnot(None),
                        Card.id.isnot(None),
                        Loan.id.isnot(None)
                    ), Account.client_id
                )))).label('all_products')
            ).outerjoin(Card, Account.account_number == Card.account_number)
            .outerjoin(Loan, Account.account_number == Loan.account_number)
        ).first()

        # Calculate product adoption over time
        adoption_stats = self.db.execute(
            select(
                func.date_trunc('month', Account.creation_date).label('month'),
                func.count(func.distinct(Account.client_id)).label('new_clients'),
                func.count(func.distinct(case((Card.id.isnot(None), Account.client_id)))).label('new_card_users'),
                func.count(func.distinct(case((Loan.id.isnot(None), Account.client_id)))).label('new_loan_users')
            ).outerjoin(Card, Account.account_number == Card.account_number)
            .outerjoin(Loan, Account.account_number == Loan.account_number)
            .group_by('month')
            .order_by('month')
        ).all()

        # Calculate segment-based product usage
        segment_stats = self.db.execute(
            select(
                case(
                    (Account.balance > 10000, 'Premium'),
                    (Account.balance > 5000, 'Standard'),
                    else_='Basic'
                ).label('segment'),
                func.count(func.distinct(Account.client_id)).label('client_count'),
                func.count(Card.id).label('card_count'),
                func.count(Loan.id).label('loan_count'),
                func.avg(Account.balance).label('avg_balance'),
                func.avg(case((Card.type == CardType.CREDIT, Card.card_limit))).label('avg_credit_limit')
            ).outerjoin(Card, Account.account_number == Card.account_number)
            .outerjoin(Loan, Account.account_number == Loan.account_number)
            .group_by('segment')
        ).all()

        return {
            'total_clients': total_clients,
            'account_engagement': {
                'clients_with_accounts': account_stats.clients_with_accounts,
                'total_accounts': account_stats.total_accounts,
                'foreign_accounts': account_stats.foreign_accounts,
                'current_accounts': account_stats.current_accounts,
                'avg_balance': float(account_stats.avg_balance) if account_stats.avg_balance else 0,
                'account_penetration_rate': round(
                    account_stats.clients_with_accounts / total_clients if total_clients > 0 else 0, 2)
            },
            'card_engagement': {
                'clients_with_cards': card_stats.clients_with_cards,
                'total_cards': card_stats.total_cards,
                'credit_cards': card_stats.credit_cards,
                'debit_cards': card_stats.debit_cards,
                'avg_credit_limit': float(card_stats.avg_credit_limit) if card_stats.avg_credit_limit else 0,
                'card_penetration_rate': round(
                    card_stats.clients_with_cards / total_clients if total_clients > 0 else 0, 2)
            },
            'payment_activity': {
                'active_accounts': payment_stats.active_accounts,
                'total_payments': payment_stats.total_payments,
                'avg_payment_amount': round(
                    float(payment_stats.avg_payment_amount) if payment_stats.avg_payment_amount else 0, 2),
                'total_completed_amount': float(
                    payment_stats.total_completed_amount) if payment_stats.total_completed_amount else 0,
                'failed_payments': payment_stats.failed_payments,
                'payment_success_rate': round(1 - (
                    payment_stats.failed_payments / payment_stats.total_payments if payment_stats.total_payments > 0 else 0),
                                              2)
            },
            'product_correlations': {
                'accounts_with_cards': correlation_stats.accounts_with_cards,
                'accounts_with_loans': correlation_stats.accounts_with_loans,
                'cards_with_loans': correlation_stats.cards_with_loans,
                'all_products': correlation_stats.all_products,
                'correlation_rates': {
                    'account_card_correlation': round(
                        correlation_stats.accounts_with_cards / account_stats.clients_with_accounts if account_stats.clients_with_accounts > 0 else 0,
                        2),
                    'account_loan_correlation': round(
                        correlation_stats.accounts_with_loans / account_stats.clients_with_accounts if account_stats.clients_with_accounts > 0 else 0,
                        2),
                    'card_loan_correlation': round(
                        correlation_stats.cards_with_loans / card_stats.clients_with_cards if card_stats.clients_with_cards > 0 else 0,
                        2),
                    'full_product_correlation': round(
                        correlation_stats.all_products / total_clients if total_clients > 0 else 0, 2)
                }
            },
            'product_adoption': [
                {
                    'month': str(stat.month),
                    'new_clients': stat.new_clients,
                    'new_card_users': stat.new_card_users,
                    'new_loan_users': stat.new_loan_users,
                    'card_adoption_rate': round(stat.new_card_users / stat.new_clients if stat.new_clients > 0 else 0,
                                                2),
                    'loan_adoption_rate': round(stat.new_loan_users / stat.new_clients if stat.new_clients > 0 else 0,
                                                2)
                }
                for stat in adoption_stats
            ],
            'segment_analysis': [
                {
                    'segment': stat.segment,
                    'client_count': stat.client_count,
                    'card_count': stat.card_count,
                    'loan_count': stat.loan_count,
                    'avg_balance': float(stat.avg_balance) if stat.avg_balance else 0,
                    'avg_credit_limit': float(stat.avg_credit_limit) if stat.avg_credit_limit else 0,
                    'cards_per_client': round(stat.card_count / stat.client_count if stat.client_count > 0 else 0, 2),
                    'loans_per_client': round(stat.loan_count / stat.client_count if stat.client_count > 0 else 0, 2)
                }
                for stat in segment_stats
            ],
            'loan_activity': loan_stats
        }

    def get_product_combinations(self):
        """Analyze which products are commonly used together"""
        # Get clients with their account activity patterns
        query = select(
            Account.client_id,
            func.count(func.distinct(Account.account_number)).label('has_accounts'),
            func.count(func.distinct(case(
                (Account.type == AccountType.FOREIGN, Account.account_number)
            ))).label('has_foreign_account'),
            func.count(func.distinct(case(
                (and_(Payment.amount > 1000, Payment.status == PaymentStatus.COMPLETED), Payment.id)
            ))).label('has_large_transactions')
        ).outerjoin(Payment, Account.account_number == Payment.sender_account_number) \
            .group_by(Account.client_id)

        result = self.db.execute(query).all()

        # Convert to DataFrame for analysis
        df = pd.DataFrame(result,
                          columns=['client_id', 'has_accounts', 'has_foreign_account', 'has_large_transactions'])
        df['has_multiple_accounts'] = df['has_accounts'] > 1
        df['has_foreign_account'] = df['has_foreign_account'] > 0
        df['has_large_transactions'] = df['has_large_transactions'] > 0

        # Calculate account combinations
        combinations = {
            'multiple_accounts_and_large_trans': int(
                df[(df['has_multiple_accounts']) & (df['has_large_transactions'])].shape[0]),
            'foreign_and_large_trans': int(df[(df['has_foreign_account']) & (df['has_large_transactions'])].shape[0]),
            'only_multiple_accounts': int(df[(df['has_multiple_accounts']) & (~df['has_large_transactions'])].shape[0]),
            'only_single_account': int(df[(~df['has_multiple_accounts']) & (~df['has_large_transactions'])].shape[0])
        }

        return combinations
