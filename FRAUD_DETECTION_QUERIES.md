# Fraud Detection Ontology - Query Guide

## What Gets Indexed from `fraud-detection-demo.owl`

### Classes with Hierarchy
```
OWL Class: CircularMoneyFlow subClassOf FraudPattern
OWL Class: FraudPattern
OWL Class: Account
OWL Class: DetectionAlgorithm
OWL Class: RiskLevel
```

### Critical Individual: CircularFlow_001
```
Individual: CircularFlow_001 
  type:CircularMoneyFlow 
  detectedBy:DFS_Algorithm 
  hasRiskLevel:HighRisk 
  riskScore:'0.95' 
  totalAmount:'200000.00'
```

### Detection Algorithm
```
Individual: DFS_Algorithm type:DetectionAlgorithm
```

### Risk Level
```
Individual: HighRisk type:RiskLevel
```

### Accounts Involved
```
Individual: Account_A 
  type:Account 
  accountId:'ACC-001' 
  balance:'5000.00' 
  sendsMoneyTo:Account_B 
  involvedIn:CircularFlow_001

Individual: Account_B 
  type:Account 
  accountId:'ACC-002' 
  balance:'1000.00' 
  sendsMoneyTo:Account_C 
  involvedIn:CircularFlow_001

Individual: Account_C 
  type:Account 
  accountId:'ACC-003' 
  balance:'200.00' 
  sendsMoneyTo:Account_D 
  involvedIn:CircularFlow_001

Individual: Account_D 
  type:Account 
  accountId:'ACC-004' 
  balance:'50.00' 
  sendsMoneyTo:Account_A 
  involvedIn:CircularFlow_001
```

### Object Properties
```
OWL Object Property: sendsMoneyTo domain:Account range:Account
OWL Object Property: involvedIn domain:Account range:FraudPattern
OWL Object Property: detectedBy domain:FraudPattern range:DetectionAlgorithm
OWL Object Property: hasRiskLevel domain:FraudPattern range:RiskLevel
```

---

## Example Queries & Expected Answers

### ✅ Query 1: "Who created CircularFlow_001?" OR "Who detected CircularFlow_001?"

**Retrieved Context:**
```
Individual: CircularFlow_001 type:CircularMoneyFlow detectedBy:DFS_Algorithm hasRiskLevel:HighRisk riskScore:'0.95' totalAmount:'200000.00'
Individual: DFS_Algorithm type:DetectionAlgorithm
OWL Object Property: detectedBy domain:FraudPattern range:DetectionAlgorithm
```

**Expected Answer (with enhanced prompt):**
```
CircularFlow_001 was detected (not "created") by the DFS_Algorithm.

Details:
- Pattern ID: CircularFlow_001
- Type: CircularMoneyFlow (a subclass of FraudPattern)
- Detected by: DFS_Algorithm (a DetectionAlgorithm)
- Risk Level: HighRisk
- Risk Score: 0.95 (very high)
- Total Amount: $200,000.00

Note: The ontology uses the relationship "detectedBy" rather than "createdBy" 
because fraud patterns are detected/discovered by algorithms, not created by them.
```

---

### ✅ Query 2: "What accounts are involved in CircularFlow_001?"

**Retrieved Context:**
```
Individual: Account_A type:Account involvedIn:CircularFlow_001 sendsMoneyTo:Account_B accountId:'ACC-001' balance:'5000.00'
Individual: Account_B type:Account involvedIn:CircularFlow_001 sendsMoneyTo:Account_C accountId:'ACC-002' balance:'1000.00'
Individual: Account_C type:Account involvedIn:CircularFlow_001 sendsMoneyTo:Account_D accountId:'ACC-003' balance:'200.00'
Individual: Account_D type:Account involvedIn:CircularFlow_001 sendsMoneyTo:Account_A accountId:'ACC-004' balance:'50.00'
```

**Expected Answer:**
```
Four accounts are involved in CircularFlow_001, forming a circular money flow pattern:

1. Account_A (ACC-001)
   - Balance: $5,000.00
   - Sends money to: Account_B

2. Account_B (ACC-002)
   - Balance: $1,000.00
   - Sends money to: Account_C

3. Account_C (ACC-003)
   - Balance: $200.00
   - Sends money to: Account_D

4. Account_D (ACC-004)
   - Balance: $50.00
   - Sends money to: Account_A (completes the circle)

Money Flow Cycle:
Account_A → Account_B → Account_C → Account_D → Account_A

This circular pattern with a total amount of $200,000.00 was flagged as high risk 
(score: 0.95) and detected by the DFS_Algorithm.
```

---

### ✅ Query 3: "What is the risk score of the circular money flow?"

**Retrieved Context:**
```
Individual: CircularFlow_001 type:CircularMoneyFlow hasRiskLevel:HighRisk riskScore:'0.95' totalAmount:'200000.00'
Individual: HighRisk type:RiskLevel
```

**Expected Answer:**
```
The circular money flow pattern (CircularFlow_001) has:

Risk Score: 0.95 (on a scale of 0.0 to 1.0)
Risk Level: HighRisk
Total Amount: $200,000.00

This 0.95 score indicates an extremely high risk of fraudulent activity, 
close to the maximum possible risk score.
```

---

### ✅ Query 4: "Tell me about the DFS Algorithm"

**Retrieved Context:**
```
Individual: DFS_Algorithm type:DetectionAlgorithm
Individual: CircularFlow_001 type:CircularMoneyFlow detectedBy:DFS_Algorithm
OWL Object Property: detectedBy domain:FraudPattern range:DetectionAlgorithm
```

**Expected Answer:**
```
DFS_Algorithm is a DetectionAlgorithm used in fraud detection.

Key Information:
- Type: DetectionAlgorithm
- Purpose: Detects fraud patterns in financial transactions

Detections:
The DFS_Algorithm detected:
- CircularFlow_001: A CircularMoneyFlow pattern involving 4 accounts 
  with a risk score of 0.95 and total amount of $200,000.00

DFS likely stands for "Depth-First Search," a graph algorithm commonly 
used to detect cycles (circular flows) in network structures.
```

---

### ✅ Query 5: "What fraud patterns exist?"

**Retrieved Context:**
```
OWL Class: CircularMoneyFlow subClassOf FraudPattern
Individual: CircularFlow_001 type:CircularMoneyFlow detectedBy:DFS_Algorithm hasRiskLevel:HighRisk
```

**Expected Answer:**
```
The ontology defines fraud patterns:

Fraud Pattern Types:
1. CircularMoneyFlow (subclass of FraudPattern)
   - Description: Money flowing in a circular pattern between accounts
   
Detected Instances:
1. CircularFlow_001
   - Type: CircularMoneyFlow
   - Risk: HighRisk (score: 0.95)
   - Amount: $200,000.00
   - Detected by: DFS_Algorithm
   - Involves: 4 accounts (ACC-001, ACC-002, ACC-003, ACC-004)
```

---

## Common Question Variations That Now Work

The enhanced system prompt maps semantic variations:

| User Says | Ontology Has | LLM Should Understand |
|-----------|--------------|----------------------|
| "who **created** CircularFlow?" | `detectedBy` | Detection algorithm found it |
| "who **made** the pattern?" | `detectedBy` | DFS_Algorithm detected it |
| "who **found** circular flow?" | `detectedBy` | ✅ Direct match |
| "what accounts are **involved**?" | `involvedIn` | ✅ Direct match |
| "which accounts **participate**?" | `involvedIn` | Accounts involved in pattern |
| "what's **connected** to Account_A?" | `sendsMoneyTo` | Shows money transfer relationships |

---

## Testing Steps

1. **Load fraud-detection-demo.owl** in Protégé

2. **Index the ontology**:
   - Open RAG plugin
   - Click "Index Current Ontology"
   - Wait for completion

3. **Try these queries**:
   - ✅ "Who detected CircularFlow_001?"
   - ✅ "What accounts are involved in the circular flow?"
   - ✅ "What is the risk score?"
   - ✅ "Tell me about the DFS algorithm"
   - ✅ "What fraud patterns exist?"

4. **Verify retrieved context includes relationships**:
   - Check that `detectedBy:DFS_Algorithm` appears
   - Check that `involvedIn:CircularFlow_001` appears for accounts
   - Check that `sendsMoneyTo:Account_X` appears

---

## Why Enhanced Prompt Helps

**Before (Generic Prompt):**
- LLM sees "created" in question
- Looks for "created" or "CREATED_BY" in context
- Doesn't find it
- Returns generic answer about economic concepts

**After (Enhanced Prompt with Synonym Mapping):**
- LLM sees "created" in question
- Prompt explicitly says: map "created" → check "detectedBy", "discoveredBy", "identifiedBy"
- LLM finds `detectedBy:DFS_Algorithm` in context
- Returns accurate answer using actual ontology relationships

This is **true GraphRAG** - understanding the semantic intent while respecting the actual graph structure!
