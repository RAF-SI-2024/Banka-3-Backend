from app.analysis.database import Base, engine
from app.analysis.models import Currency, Account, Card, Payment, Payee


def init_db():
    # Create all tables
    Base.metadata.create_all(bind=engine)
    print("Database tables created successfully!")


if __name__ == "__main__":
    init_db()
