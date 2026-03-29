package com.example.edi.claims.domain.loop;

import com.example.edi.common.edi.loop.SubscriberLoop;
import java.util.List;

public record SubscriberGroup(
    SubscriberLoop subscriber,
    List<ClaimLoop> claims
) {}
