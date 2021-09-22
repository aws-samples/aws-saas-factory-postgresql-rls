/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory.pgrls;

import com.amazon.aws.partners.saasfactory.pgrls.domain.Status;
import com.amazon.aws.partners.saasfactory.pgrls.domain.Tenant;
import com.amazon.aws.partners.saasfactory.pgrls.domain.Tier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.*;

public class TenantTest {

	@Test
	public void isLightweight() {
		Tenant lightweight = new Tenant();
		lightweight.setId(UUID.randomUUID());
		assertTrue("Only ID is lightweight", lightweight.isLightweight());

		lightweight.setUsers(new ArrayList<>());
		assertTrue("Only ID and empty users list is still lightweight", lightweight.isLightweight());

		Tenant heavyweight = new Tenant();
		heavyweight.setId(UUID.randomUUID());
		heavyweight.setName("ABCDEF");

		assertFalse("Any property in addition to ID are not lightweight", heavyweight.isLightweight());

		heavyweight.setStatus(Status.Active);
		heavyweight.setTier(Tier.Gold);
		heavyweight.setUsers(new ArrayList<>());
		assertFalse("Fully hydrated tenants are not lightweight", heavyweight.isLightweight());
	}
}