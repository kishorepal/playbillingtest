/**
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hcl.jp.trivaldrive


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hcl.jp.trivaldrive.adapters.SkuDetailsAdapter
import hcl.jp.trivaldrive.billingrepo.localdb.AugmentedSkuDetails
import hcl.jp.trivaldrive.viewmodels.BillingViewModel
import kotlinx.android.synthetic.main.fragment_make_purchase.view.inapp_inventory
import kotlinx.android.synthetic.main.fragment_make_purchase.view.subs_inventory

/**
 * This Fragment is simply a wrapper for the inventory (i.e. items for sale). It contains two
 * [lists][RecyclerView], one for subscriptions and one for in-app products. Here again there is
 * no complicated billing logic. All the billing logic reside inside the [BillingRepository].
 * The [BillingRepository] provides a so-called [AugmentedSkuDetails] object that shows what
 * is for sale and whether the user is allowed to buy the item at this moment. E.g. if the user
 * already has a full tank of gas, then they cannot buy gas at this moment.
 */
class MakePurchaseFragment : Fragment() {

    val LOG_TAG = "MakePurchaseFragment"
    private lateinit var billingViewModel: BillingViewModel

    private val PROMO_BASE_URL = "https://play.google.com/redeem?code=%s"
    private val RC_PROMO = 1001

    private lateinit var editText : EditText
    private lateinit var btnBuyPromo : Button

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_make_purchase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated")

        val inappAdapter = object : SkuDetailsAdapter() {
            override fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
                onPurchase(view, item)
            }
        }

        val subsAdapter = object : SkuDetailsAdapter() {
            override fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
                onPurchase(view, item)
            }
        }
        attachAdapterToRecyclerView(view.inapp_inventory, inappAdapter)
        attachAdapterToRecyclerView(view.subs_inventory, subsAdapter)

        billingViewModel = ViewModelProviders.of(this).get(BillingViewModel::class.java)
        billingViewModel.inappSkuDetailsListLiveData.observe(this, Observer {
            it?.let { inappAdapter.setSkuDetailsList(it) }
        })
        billingViewModel.subsSkuDetailsListLiveData.observe(this, Observer {
            it?.let { subsAdapter.setSkuDetailsList(it) }
        })

        editText = view.findViewById(R.id.et_promocode)
        btnBuyPromo = view.findViewById(R.id.btn_promo)
        btnBuyPromo.setOnClickListener {
            val url = String.format(PROMO_BASE_URL, editText.text.toString())
            val intent : Intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
            }

            startActivityForResult(intent, RC_PROMO)
        }

    }

    private fun attachAdapterToRecyclerView(recyclerView: RecyclerView, skuAdapter: SkuDetailsAdapter) {
        with(recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = skuAdapter
        }
    }

    private fun onPurchase(view: View, item: AugmentedSkuDetails) {
        billingViewModel.makePurchase(activity as Activity, item)
        Log.d(LOG_TAG, "starting purchase flow for SkuDetail:\n ${item}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(LOG_TAG, "[onActivityResult] >> IN")
        when (requestCode) {
            RC_PROMO -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(LOG_TAG, "[onActivityResult] >> SUCCESS")
                }
                else {
                    Log.d(LOG_TAG, "[onActivityResult] >> FAIL")
                }
            }
        }
    }
}
