
package com.veloop.rewards.payoutprocessing.queue;

import com.veloop.rewards.payoutprocessing.entity.PayoutJob;

public interface PayoutQueue {

    PayoutJob enqueue(Long withdrawalId);
}