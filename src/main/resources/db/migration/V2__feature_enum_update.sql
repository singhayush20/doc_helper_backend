-- Add new value to feature_type enum
ALTER TYPE feature_type ADD VALUE IF NOT EXISTS 'POST_GENERATOR';

-- Add new value to feature_codes enum
ALTER TYPE feature_codes ADD VALUE IF NOT EXISTS 'POST_GENERATOR';