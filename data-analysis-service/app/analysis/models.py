from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Enum, Numeric, BigInteger, Boolean
from sqlalchemy.orm import relationship, declared_attr
from datetime import datetime
import enum
from .database import Base


class AccountType(enum.Enum):
    CURRENT = "CURRENT"
    FOREIGN = "FOREIGN"


class AccountStatus(enum.Enum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"


class AccountOwnerType(enum.Enum):
    PERSONAL = "PERSONAL"
    COMPANY = "COMPANY"


class PaymentType(enum.Enum):
    DEPOSIT = "DEPOSIT"
    WITHDRAWAL = "WITHDRAWAL"
    TRANSFER = "TRANSFER"
    PAYMENT = "PAYMENT"


class PaymentStatus(enum.Enum):
    PENDING = "PENDING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class CardType(enum.Enum):
    CREDIT = "CREDIT"
    DEBIT = "DEBIT"


class CardStatus(enum.Enum):
    ACTIVE = "ACTIVE"
    INACTIVE = "INACTIVE"
    BLOCKED = "BLOCKED"


class CardIssuer(enum.Enum):
    VISA = "VISA"
    MASTERCARD = "MASTERCARD"
    DINA = "DINA"
    AMERICAN_EXPRESS = "AMERICAN_EXPRESS"


class Currency(Base):
    __tablename__ = "currencies"

    id = Column(BigInteger, primary_key=True)
    code = Column(String, unique=True)
    name = Column(String)
    symbol = Column(String)
    countries = Column(String)
    description = Column(String)
    active = Column(Boolean, default=True)

    # Relationships
    accounts = relationship("Account", back_populates="currency", foreign_keys="[Account.currency_id]")


class Account(Base):
    __tablename__ = "accounts"

    account_number = Column(String, primary_key=True)
    name = Column(String)
    client_id = Column(BigInteger)
    created_by_employee_id = Column(BigInteger)
    creation_date = Column(DateTime)
    expiration_date = Column(DateTime)
    currency_id = Column(BigInteger, ForeignKey("currencies.id"))
    status = Column(Enum(AccountStatus))
    type = Column(Enum(AccountType))
    account_owner_type = Column(Enum(AccountOwnerType))
    balance = Column(Numeric(20, 2))
    available_balance = Column(Numeric(20, 2))
    daily_limit = Column(Numeric(20, 2))
    monthly_limit = Column(Numeric(20, 2))
    daily_spending = Column(Numeric(20, 2))
    monthly_spending = Column(Numeric(20, 2))

    # Foreign key columns need to be declared with @declared_attr
    @declared_attr
    def currency_code(cls):
        return Column(String, ForeignKey("currencies.code"))

    # Relationships also need to be declared with @declared_attr
    @declared_attr
    def currency(cls):
        return relationship("Currency", back_populates="accounts", foreign_keys=[cls.currency_id])

    @declared_attr
    def cards(cls):
        return relationship("Card", back_populates="account")

    @declared_attr
    def payments_sent(cls):
        return relationship("Payment", back_populates="sender_account")

    __mapper_args__ = {
        'polymorphic_on': account_owner_type,
        'polymorphic_identity': None
    }


class PersonalAccount(Account):
    """Personal bank account for individual clients"""
    __mapper_args__ = {
        'polymorphic_identity': AccountOwnerType.PERSONAL
    }

    def __init__(self, **kwargs):
        kwargs['account_owner_type'] = AccountOwnerType.PERSONAL
        super().__init__(**kwargs)


class CompanyAccount(Account):
    """Company bank account for business clients"""
    __mapper_args__ = {
        'polymorphic_identity': AccountOwnerType.COMPANY
    }

    def __init__(self, **kwargs):
        kwargs['account_owner_type'] = AccountOwnerType.COMPANY
        super().__init__(**kwargs)


class Card(Base):
    __tablename__ = "cards"

    id = Column(BigInteger, primary_key=True)
    card_number = Column(String(16), unique=True)
    cvv = Column(String(3))
    type = Column(Enum(CardType))
    issuer = Column(Enum(CardIssuer))
    name = Column(String)
    creation_date = Column(DateTime)
    expiration_date = Column(DateTime)
    account_number = Column(String, ForeignKey("accounts.account_number"))
    status = Column(Enum(CardStatus))
    card_limit = Column(Numeric(20, 2))

    # Relationships
    account = relationship("Account", back_populates="cards")
    payments = relationship("Payment", back_populates="card")


class Payee(Base):
    __tablename__ = "payees"

    id = Column(BigInteger, primary_key=True)
    name = Column(String)
    account_number = Column(String)
    reference_number = Column(String)
    payment_code = Column(String)

    # Relationships
    payments = relationship("Payment", back_populates="payee")


class Payment(Base):
    __tablename__ = "payments"

    id = Column(BigInteger, primary_key=True)
    sender_name = Column(String)
    client_id = Column(BigInteger, nullable=False)
    sender_account_number = Column(String, ForeignKey("accounts.account_number"), nullable=False)
    card_id = Column(BigInteger, ForeignKey("cards.id"))
    amount = Column(Numeric(20, 2), nullable=False)
    account_number_receiver = Column(String)
    payee_id = Column(BigInteger, ForeignKey("payees.id"))
    payment_code = Column(String)
    purpose_of_payment = Column(String)
    reference_number = Column(String)
    date = Column(DateTime, default=datetime.utcnow)
    out_amount = Column(Numeric(20, 2))
    status = Column(Enum(PaymentStatus))
    receiver_client_id = Column(BigInteger)
    exchange_profit = Column(Numeric(20, 2))

    # Relationships
    sender_account = relationship("Account", back_populates="payments_sent")
    card = relationship("Card", back_populates="payments")
    payee = relationship("Payee", back_populates="payments")
