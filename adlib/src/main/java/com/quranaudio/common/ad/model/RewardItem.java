package com.quranaudio.common.ad.model;

import androidx.annotation.NonNull;

/**
 * create by microspark 10/9/22
 **/
public abstract class RewardItem {
 abstract public int getAmount();
  @NonNull
  abstract public String getType();
}
