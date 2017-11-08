import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        int i = 0;
        double txSum = 0;
        UTXOPool usedUTXOs = new UTXOPool();
        for (Transaction.Input in : tx.getInputs()) {
            // Unspent transaction corresponding to the output this input claims
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = utxoPool.getTxOutput(utxo);
            // if that unspent transaction is not in the pool, return false
            if (out == null) return false;
            // only valid if the input is properly signed
            if (!Crypto.verifySignature(out.address, tx.getRawDataToSign(i), in.signature)) return false;
            // if this UTXO has been used already
            if (usedUTXOs.contains(utxo)) return false;
            usedUTXOs.addUTXO(utxo, out);
            txSum += out.value;
            i++;
        }
        double outSum = 0;
        for (Transaction.Output out : tx.getOutputs()) {
            if (out.value < 0) return false;
            outSum += out.value;
        }
        return txSum >= outSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Set<Transaction> validTx = new HashSet<>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTx.add(tx);
                for (Transaction.Input in: tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int i = 0; i < tx.numOutputs(); i++) {
                    Transaction.Output out = tx.getOutput(i);
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }
        Transaction[] validTxArray = new Transaction[validTx.size()];
        return validTx.toArray(validTxArray);
    }
}
